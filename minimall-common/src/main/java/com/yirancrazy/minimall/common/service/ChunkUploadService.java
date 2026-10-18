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
 * @Description: ChunkUploadService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class ChunkUploadService {


    private static final String DEFAULT_ROOT = System.getProperty("java.io.tmpdir") + "/mm-chunk";

    private static final String OWNER_FILE = ".owner";

    private static final UploadLimits DEFAULT_LIMITS = new UploadLimits(
        1000, 10L * 1024 * 1024, 2L * 1024 * 1024 * 1024,
        10L * 1024 * 1024 * 1024, Duration.ofHours(24));

    private final MinioUtil minioUtil;
    private final Path root;
    private final UploadLimits limits;
    private final Clock clock;

    /**
     * 构造 ChunkUploadService 实例。
     * @param minioUtil minioUtil 参数
     */
    public ChunkUploadService(MinioUtil minioUtil) {
        this(minioUtil, Paths.get(DEFAULT_ROOT), DEFAULT_LIMITS, Clock.systemUTC());
    }


    /**
     * 构造 ChunkUploadService 实例。
     * @param minioUtil minioUtil 参数
     * @param root root 参数
     * @param limits limits 参数
     * @param clock clock 参数
     */
    public ChunkUploadService(MinioUtil minioUtil, Path root, UploadLimits limits, Clock clock) {
        this.minioUtil = minioUtil;
        this.root = root.toAbsolutePath().normalize();
        this.limits = limits;
        this.clock = clock;
    }


    /**
     * 保存业务数据。
     * @param userId userId 参数
     * @param uploadId uploadId 参数
     * @param chunkIndex chunkIndex 参数
     * @param totalChunks totalChunks 参数
     * @param contentType contentType 参数
     * @param fileName fileName 参数
     * @param in in 参数
     * @return 处理结果
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
     * 保存业务数据。
     * @param userId userId 参数
     * @param uploadId uploadId 参数
     * @param chunkIndex chunkIndex 参数
     * @param totalChunks totalChunks 参数
     * @param contentType contentType 参数
     * @param fileName fileName 参数
     * @param streamSupplier streamSupplier 参数
     * @return 处理结果
     */
    public ChunkResult saveChunk(Long userId, String uploadId, int chunkIndex, int totalChunks,
                                 String contentType, String fileName, ChunkInputStreamSupplier streamSupplier) {
        try (InputStream in = streamSupplier.open()) {
            return saveChunk(userId, uploadId, chunkIndex, totalChunks, contentType, fileName, in);
        }
        catch (IOException e) {
            throw new BizException(UploadCodeEnum.CHUNK_UPLOAD_FAIL);
        }
    }


    /**
     * @Author: yirancrazy@gmail.com
     * @Description: ChunkInputStreamSupplier 接口，定义相关能力。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    @FunctionalInterface
    public interface ChunkInputStreamSupplier {


        /**
         * 执行 open 方法。
         * @return 处理结果
         * @throws IOException 处理失败时抛出
         */
        InputStream open() throws IOException;
    }


    /**
     * 执行 receivedChunks 方法。
     * @param userId userId 参数
     * @param uploadId uploadId 参数
     * @param totalChunks totalChunks 参数
     * @return 结果列表
     */
    public List<Integer> receivedChunks(Long userId, String uploadId, int totalChunks) {
        Path dir = taskDir(uploadId);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try {
            /**
             * 执行 ensureOwner 方法。
             * @param userId userId 参数
             * @param dir dir 参数
             */
            ensureOwner(userId, dir);
        }
        catch (IOException e) {
            throw new BizException(UploadCodeEnum.CHUNK_UPLOAD_FAIL);
        }
        /**
         * 执行 try 方法。
         */
        try (var stream = Files.list(dir)) {
            return stream
                /**
                 * 执行 filter 方法。
                 * @return 处理结果
                 */
                .filter(p -> p.getFileName().toString().startsWith("chunk-"))
                /**
                 * 执行 map 方法。
                 * @return 处理结果
                 */
                .map(p -> p.getFileName().toString().substring("chunk-".length()))
                /**
                 * 执行 filter 方法。
                 * @return 处理结果
                 */
                .filter(s -> s.matches("\\d+"))
                /**
                 * 执行 map 方法。
                 * @return 处理结果
                 */
                .map(Integer::parseInt)
                /**
                 * 执行 filter 方法。
                 * @return 处理结果
                 */
                .filter(i -> i >= 0 && i < totalChunks)
                /**
                 * 执行 sorted 方法。
                 * @return 处理结果
                 */
                .sorted()
                /**
                 * 执行 collect 方法。
                 * @return 处理结果
                 */
                .collect(Collectors.toList());
        }
        catch (IOException e) {
            throw new BizException(UploadCodeEnum.CHUNK_UPLOAD_FAIL);
        }
    }


    /**
     * 删除业务数据。
     * @param uploadId uploadId 参数
     */
    public void deleteTask(String uploadId) {
        deleteDir(taskDir(uploadId));
    }


    /**
     * 执行 cleanupExpiredTasks 方法。
     * @return 业务数值
     */
    public int cleanupExpiredTasks() {
        if (!Files.isDirectory(root)) {
            return 0;
        }
        long expireBefore = clock.instant().minus(limits.taskTtl()).toEpochMilli();
        int cleaned = 0;
        /**
         * 执行 try 方法。
         */
        try (var dirs = Files.list(root)) {
            List<Path> expired = dirs
                /**
                 * 执行 filter 方法。
                 * @param isDirectory isDirectory 参数
                 * @return 处理结果
                 */
                .filter(Files::isDirectory)
                /**
                 * 执行 filter 方法。
                 * @return 处理结果
                 */
                .filter(dir -> {
                    try {
                        return Files.getLastModifiedTime(dir).toMillis() < expireBefore;
                    }
                    catch (IOException e) {
                        return false;
                    }
                })
                /**
                 * 执行 collect 方法。
                 * @return 处理结果
                 */
                .collect(Collectors.toList());
            for (Path dir : expired) {
                /**
                 * 删除业务数据。
                 * @param dir dir 参数
                 */
                deleteDir(dir);
                cleaned++;
            }
            return cleaned;
        }
        catch (IOException e) {
            throw new BizException(UploadCodeEnum.CHUNK_UPLOAD_FAIL);
        }
    }

    /**
     * 校验Params。
     * @param uploadId 上传 ID
     * @param chunkIndex chunkIndex
     * @param totalChunks totalChunks
     * @throws BizException 业务校验失败时抛出
     */
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

    /**
     * 处理任务Dir。
     * @param uploadId 上传 ID
     * @return 处理结果
     * @throws BizException 业务校验失败时抛出
     */
    private Path taskDir(String uploadId) {
        Path dir = root.resolve(uploadId).normalize();
        // 双保险：即使字符集校验被绕过，normalize 后越出 ROOT 的路径也直接拒绝
        if (!dir.startsWith(root) || dir.getNameCount() != root.getNameCount() + 1) {
            throw new BizException(UploadCodeEnum.CHUNK_PARAM_INVALID);
        }
        return dir;
    }

    /**
     * 处理ensureOwner。
     * @param userId 用户 ID
     * @param dir dir
     * @throws IOException 处理失败时抛出
     * @throws BizException 业务校验失败时抛出
     */
    private void ensureOwner(Long userId, Path dir) throws IOException {
        Path ownerFile = dir.resolve(OWNER_FILE);
        try {
            /**
             * 执行 writeString 方法。
             * @param ownerFile ownerFile 参数
             * @param CREATE_NEW CREATE_NEW 参数
             * @param WRITE WRITE 参数
             * @return 处理结果
             */
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

    /**
     * 处理storedBytes。
     * @param dir dir
     * @return 处理结果
     * @throws IOException 处理失败时抛出
     */
    private long storedBytes(Path dir) throws IOException {
        /**
         * 执行 try 方法。
         */
        try (var files = Files.list(dir)) {
            return files
                /**
                 * 执行 filter 方法。
                 * @return 处理结果
                 */
                .filter(path -> path.getFileName().toString().startsWith("chunk-"))
                /**
                 * 执行 mapToLong 方法。
                 * @return 处理结果
                 */
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    }
                    catch (IOException e) {
                        return 0L;
                    }
                })
                /**
                 * 执行 sum 方法。
                 * @return 处理结果
                 */
                .sum();
        }
    }

    /**
     * 处理用户StoredBytes。
     * @param userId 用户 ID
     * @return 处理结果
     * @throws IOException 处理失败时抛出
     */
    private long userStoredBytes(Long userId) throws IOException {
        if (!Files.isDirectory(root)) {
            return 0L;
        }
        long total = 0L;
        /**
         * 执行 try 方法。
         */
        try (var dirs = Files.list(root)) {
            for (Path dir : dirs.filter(Files::isDirectory).collect(Collectors.toList())) {
                Path ownerFile = dir.resolve(OWNER_FILE);
                if (!Files.isRegularFile(ownerFile)
                    /**
                     * 执行 valueOf 方法。
                     * @return 处理结果
                     */
                    || !String.valueOf(userId).equals(Files.readString(ownerFile).trim())) {
                    continue;
                }
                total += storedBytes(dir);
            }
        }
        return total;
    }


    /**
     * 填充或标记And上传。
     * @param uploadId 上传 ID
     * @param totalChunks totalChunks
     * @param contentType contentType
     * @param fileName 文件Name
     * @return 处理结果
     * @throws BizException 业务校验失败时抛出
     */
    private String mergeAndUpload(String uploadId, int totalChunks, String contentType, String fileName) {
        Path dir = taskDir(uploadId);
        Path merged = dir.resolve("merged.bin");
        try {
            // 按序号拼接分片到合并文件，避免一次性持有多个流
            /**
             * 执行 try 方法。
             */
            try (OutputStream out = Files.newOutputStream(
                    merged,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                for (int i = 0; i < totalChunks; i++) {
                    Path chunk = dir.resolve("chunk-" + i);
                    /**
                     * 执行 try 方法。
                     */
                    try (InputStream in = Files.newInputStream(chunk)) {
                        /**
                         * 执行 transferTo 方法。
                         * @param out out 参数
                         * @return 处理结果
                         */
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
            /**
             * 删除业务数据。
             * @param dir dir 参数
             */
            deleteDir(dir);
        }
    }


    /**
     * 处理extensionOf。
     * @param fileName 文件Name
     * @return 处理结果
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

    /**
     * 删除Dir。
     * @param dir dir
     */
    private static void deleteDir(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return;
        }
        /**
         * 执行 try 方法。
         */
        try (var stream = Files.walk(dir)) {
            /**
             * 执行 sorted 方法。
             * @return 处理结果
             */
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    /**
                     * 删除业务数据。
                     * @param p p 参数
                     * @return 处理结果
                     */
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


    /**
     * @Author: yirancrazy@gmail.com
     * @Description: ChunkResult 记录对象，承载不可变数据。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    public record ChunkResult(boolean done, int received, String objectKey) {
    }


    /**
     * @Author: yirancrazy@gmail.com
     * @Description: UploadLimits 记录对象，承载不可变数据。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    public record UploadLimits(int maxTotalChunks, long maxChunkBytes, long maxTaskBytes,
                               long maxUserBytes, Duration taskTtl) {
    }
}
