package com.yirancrazy.minimall.common.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.yirancrazy.minimall.common.constant.UploadCodeEnum;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.util.MinioUtil;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 分片上传服务，分片落本地临时目录，全部到齐后合并为临时文件并上传 MinIO。
 *  由 MinioConfig 注册为 Bean，仅在 minimall.minio.endpoint 配置存在时可用。
 * @Version: 1.0
 * @DateTime: 2026/08/12
 **/
public class ChunkUploadService {

    /** 分片任务根目录：{tmp}/mm-chunk/{uploadId} */
    private static final String DEFAULT_ROOT = System.getProperty("java.io.tmpdir") + "/mm-chunk";

    private static final String OWNER_FILE = ".owner";

    private static final UploadLimits DEFAULT_LIMITS = new UploadLimits(
        1000, 10L * 1024 * 1024, 2L * 1024 * 1024 * 1024,
        10L * 1024 * 1024 * 1024, Duration.ofHours(24));

    private final MinioUtil minioUtil;
    private final Path root;
    private final UploadLimits limits;
    private final Clock clock;

    public ChunkUploadService(MinioUtil minioUtil) {
        this(minioUtil, Paths.get(DEFAULT_ROOT), DEFAULT_LIMITS, Clock.systemUTC());
    }

    /**
     * 创建支持配额、用户隔离和过期清理的分片上传服务。
     * @param minioUtil 对象存储工具
     * @param root 分片临时根目录
     * @param limits 上传限制
     * @param clock 时钟源
     */
    public ChunkUploadService(MinioUtil minioUtil, Path root, UploadLimits limits, Clock clock) {
        this.minioUtil = minioUtil;
        this.root = root.toAbsolutePath().normalize();
        this.limits = limits;
        this.clock = clock;
    }

    /**
     * 保存单个分片；若已收满全部，则合并上传 MinIO 并清理临时文件。
     * @param userId 上传用户ID
     * @param uploadId 上传任务 ID
     * @param chunkIndex 分片序号（0 起）
     * @param totalChunks 总分片数
     * @param contentType 内容类型
     * @param fileName 原始文件名，用于生成 objectKey 后缀（可空）
     * @param in 分片输入流
     * @return 上传结果；done=true 时含 objectKey
     */
    public ChunkResult saveChunk(Long userId, String uploadId, int chunkIndex, int totalChunks,
                                 String contentType, String fileName, InputStream in) {
        validateParams(uploadId, chunkIndex, totalChunks);
        if (userId == null || userId <= 0) {
            throw new BizException(UploadCodeEnum.CHUNK_PARAM_INVALID);
        }
        Path dir = taskDir(uploadId);
        Path chunkPath = dir.resolve("chunk-" + chunkIndex);
        try {
            Files.createDirectories(dir);
            ensureOwner(userId, dir);
            try (OutputStream out = Files.newOutputStream(
                    chunkPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                in.transferTo(out);
            }
            if (Files.size(chunkPath) > limits.maxChunkBytes()) {
                Files.deleteIfExists(chunkPath);
                throw new BizException(UploadCodeEnum.CHUNK_SIZE_LIMIT_EXCEEDED);
            }
            long taskBytes = storedBytes(dir);
            if (taskBytes > limits.maxTaskBytes()) {
                Files.deleteIfExists(chunkPath);
                throw new BizException(UploadCodeEnum.TASK_SIZE_LIMIT_EXCEEDED);
            }
            if (userStoredBytes(userId) > limits.maxUserBytes()) {
                Files.deleteIfExists(chunkPath);
                throw new BizException(UploadCodeEnum.USER_QUOTA_EXCEEDED);
            }
        }
        catch (BizException e) {
            throw e;
        }
        catch (IOException e) {
            throw new BizException(UploadCodeEnum.CHUNK_UPLOAD_FAIL);
        }
        List<Integer> received = receivedChunks(userId, uploadId, totalChunks);
        if (received.size() == totalChunks) {
            String objectKey = mergeAndUpload(uploadId, totalChunks, contentType, fileName);
            return new ChunkResult(true, received.size(), objectKey);
        }
        return new ChunkResult(false, received.size(), null);
    }

    /**
     * 查询已接收分片序号集合。
     * @param userId 上传用户ID
     * @param uploadId 上传任务 ID
     * @param totalChunks 总分片数
     * @return 已接收分片序号（升序）
     */
    public List<Integer> receivedChunks(Long userId, String uploadId, int totalChunks) {
        Path dir = taskDir(uploadId);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try {
            ensureOwner(userId, dir);
        }
        catch (IOException e) {
            throw new BizException(UploadCodeEnum.CHUNK_UPLOAD_FAIL);
        }
        try (var stream = Files.list(dir)) {
            return stream
                .filter(p -> p.getFileName().toString().startsWith("chunk-"))
                .map(p -> p.getFileName().toString().substring("chunk-".length()))
                .filter(s -> s.matches("\\d+"))
                .map(Integer::parseInt)
                .filter(i -> i >= 0 && i < totalChunks)
                .sorted()
                .collect(Collectors.toList());
        }
        catch (IOException e) {
            throw new BizException(UploadCodeEnum.CHUNK_UPLOAD_FAIL);
        }
    }

    /**
     * 删除整个上传任务临时目录（取消/超时清理）。
     * @param uploadId 上传任务 ID
     */
    public void deleteTask(String uploadId) {
        deleteDir(taskDir(uploadId));
    }

    /**
     * 清理超过 TTL 的未完成上传任务。
     * @return 清理任务数
     */
    public int cleanupExpiredTasks() {
        if (!Files.isDirectory(root)) {
            return 0;
        }
        long expireBefore = clock.instant().minus(limits.taskTtl()).toEpochMilli();
        int cleaned = 0;
        try (var dirs = Files.list(root)) {
            List<Path> expired = dirs
                .filter(Files::isDirectory)
                .filter(dir -> {
                    try {
                        return Files.getLastModifiedTime(dir).toMillis() < expireBefore;
                    }
                    catch (IOException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());
            for (Path dir : expired) {
                deleteDir(dir);
                cleaned++;
            }
            return cleaned;
        }
        catch (IOException e) {
            throw new BizException(UploadCodeEnum.CHUNK_UPLOAD_FAIL);
        }
    }

    private void validateParams(String uploadId, int chunkIndex, int totalChunks) {
        // uploadId 限定为字母数字与短横线，杜绝路径分隔符/相对路径穿越字符，同时要求 8~64 位
        if (totalChunks < 1 || chunkIndex < 0 || chunkIndex >= totalChunks
            || uploadId == null || !uploadId.matches("[a-zA-Z0-9-]{8,64}")) {
            throw new BizException(UploadCodeEnum.CHUNK_PARAM_INVALID);
        }
        if (totalChunks > limits.maxTotalChunks()) {
            throw new BizException(UploadCodeEnum.CHUNK_COUNT_LIMIT_EXCEEDED);
        }
    }

    private Path taskDir(String uploadId) {
        Path dir = root.resolve(uploadId).normalize();
        // 双保险：即使字符集校验被绕过，normalize 后越出 ROOT 的路径也直接拒绝
        if (!dir.startsWith(root) || dir.getNameCount() != root.getNameCount() + 1) {
            throw new BizException(UploadCodeEnum.CHUNK_PARAM_INVALID);
        }
        return dir;
    }

    private void ensureOwner(Long userId, Path dir) throws IOException {
        Path ownerFile = dir.resolve(OWNER_FILE);
        try {
            Files.writeString(ownerFile, String.valueOf(userId),
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
        catch (FileAlreadyExistsException e) {
            String owner = Files.readString(ownerFile).trim();
            if (!String.valueOf(userId).equals(owner)) {
                throw new BizException(UploadCodeEnum.UPLOAD_FORBIDDEN);
            }
        }
    }

    private long storedBytes(Path dir) throws IOException {
        try (var files = Files.list(dir)) {
            return files
                .filter(path -> path.getFileName().toString().startsWith("chunk-"))
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    }
                    catch (IOException e) {
                        return 0L;
                    }
                })
                .sum();
        }
    }

    private long userStoredBytes(Long userId) throws IOException {
        if (!Files.isDirectory(root)) {
            return 0L;
        }
        long total = 0L;
        try (var dirs = Files.list(root)) {
            for (Path dir : dirs.filter(Files::isDirectory).collect(Collectors.toList())) {
                Path ownerFile = dir.resolve(OWNER_FILE);
                if (!Files.isRegularFile(ownerFile)
                    || !String.valueOf(userId).equals(Files.readString(ownerFile).trim())) {
                    continue;
                }
                total += storedBytes(dir);
            }
        }
        return total;
    }

    /**
     * 按序号顺序合并分片为单个临时文件，上传 MinIO 后清理分片目录与合并文件。
     * @param uploadId 上传任务 ID
     * @param totalChunks 总分片数
     * @param contentType 内容类型
     * @param fileName 原始文件名，用于生成 objectKey 后缀（可空）
     * @return MinIO 对象 key
     */
    private String mergeAndUpload(String uploadId, int totalChunks, String contentType, String fileName) {
        Path dir = taskDir(uploadId);
        Path merged = dir.resolve("merged.bin");
        try {
            // 按序号拼接分片到合并文件，避免一次性持有多个流
            try (OutputStream out = Files.newOutputStream(
                    merged,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                for (int i = 0; i < totalChunks; i++) {
                    Path chunk = dir.resolve("chunk-" + i);
                    try (InputStream in = Files.newInputStream(chunk)) {
                        in.transferTo(out);
                    }
                }
            }
            return minioUtil.upload(merged, uploadId + extensionOf(fileName), contentType);
        }
        catch (IOException e) {
            throw new BizException(UploadCodeEnum.CHUNK_MERGE_FAIL);
        }
        finally {
            deleteDir(dir);
        }
    }

    /**
     * 从原始文件名提取合法后缀（如 "avatar.png" -> ".png"），无后缀或后缀非法时返回空串。
     * @param fileName 原始文件名
     * @return 小写后缀（含点），如 ".png"；无合法后缀返回 ""
     */
    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String ext = fileName.substring(dot + 1).toLowerCase();
        return ext.matches("[a-z0-9]{1,10}") ? "." + ext : "";
    }

    private static void deleteDir(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                }
                catch (IOException ignored) {
                    // 清理失败不影响主流程，留待下次清理
                }
            });
        }
        catch (IOException ignored) {
            // 目录清理失败静默
        }
    }

    /** 分片保存结果 */
    public record ChunkResult(boolean done, int received, String objectKey) {
    }

    /** 上传资源限制 */
    public record UploadLimits(int maxTotalChunks, long maxChunkBytes, long maxTaskBytes,
                               long maxUserBytes, Duration taskTtl) {
    }
}
