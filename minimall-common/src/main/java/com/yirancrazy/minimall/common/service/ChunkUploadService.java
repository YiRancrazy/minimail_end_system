package com.yirancrazy.minimall.common.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
    private static final String ROOT = System.getProperty("java.io.tmpdir") + "/mm-chunk";

    private final MinioUtil minioUtil;

    public ChunkUploadService(MinioUtil minioUtil) {
        this.minioUtil = minioUtil;
    }

    /**
     * 保存单个分片；若已收满全部，则合并上传 MinIO 并清理临时文件。
     * @param uploadId 上传任务 ID
     * @param chunkIndex 分片序号（0 起）
     * @param totalChunks 总分片数
     * @param contentType 内容类型
     * @param in 分片输入流
     * @return 上传结果；done=true 时含 objectKey
     */
    public ChunkResult saveChunk(String uploadId, int chunkIndex, int totalChunks,
                                 String contentType, InputStream in) {
        validateParams(uploadId, chunkIndex, totalChunks);
        try {
            Files.createDirectories(taskDir(uploadId));
            try (OutputStream out = Files.newOutputStream(
                    taskDir(uploadId).resolve("chunk-" + chunkIndex), StandardOpenOption.CREATE)) {
                in.transferTo(out);
            }
        }
        catch (IOException e) {
            throw new BizException(UploadCodeEnum.CHUNK_UPLOAD_FAIL);
        }
        List<Integer> received = receivedChunks(uploadId, totalChunks);
        if (received.size() == totalChunks) {
            String objectKey = mergeAndUpload(uploadId, totalChunks, contentType);
            return new ChunkResult(true, received.size(), objectKey);
        }
        return new ChunkResult(false, received.size(), null);
    }

    /**
     * 查询已接收分片序号集合。
     * @param uploadId 上传任务 ID
     * @param totalChunks 总分片数
     * @return 已接收分片序号（升序）
     */
    public List<Integer> receivedChunks(String uploadId, int totalChunks) {
        Path dir = taskDir(uploadId);
        if (!Files.isDirectory(dir)) {
            return List.of();
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

    private void validateParams(String uploadId, int chunkIndex, int totalChunks) {
        if (uploadId == null || uploadId.isBlank()
            || totalChunks < 1 || chunkIndex < 0 || chunkIndex >= totalChunks) {
            throw new BizException(UploadCodeEnum.CHUNK_PARAM_INVALID);
        }
    }

    private Path taskDir(String uploadId) {
        return Paths.get(ROOT, uploadId);
    }

    /**
     * 按序号顺序合并分片为单个临时文件，上传 MinIO 后清理分片目录与合并文件。
     * @param uploadId 上传任务 ID
     * @param totalChunks 总分片数
     * @param contentType 内容类型
     * @return MinIO objectKey
     */
    private String mergeAndUpload(String uploadId, int totalChunks, String contentType) {
        Path dir = taskDir(uploadId);
        Path merged = dir.resolve("merged.bin");
        try {
            // 按序号拼接分片到合并文件，避免一次性持有多个流
            try (OutputStream out = Files.newOutputStream(merged, StandardOpenOption.CREATE)) {
                for (int i = 0; i < totalChunks; i++) {
                    Path chunk = dir.resolve("chunk-" + i);
                    try (InputStream in = Files.newInputStream(chunk)) {
                        in.transferTo(out);
                    }
                }
            }
            return minioUtil.upload(merged, uploadId, contentType);
        }
        catch (IOException e) {
            throw new BizException(UploadCodeEnum.CHUNK_MERGE_FAIL);
        }
        finally {
            deleteDir(dir);
        }
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
}
