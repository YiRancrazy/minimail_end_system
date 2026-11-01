package com.yirancrazy.minimall.user.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.service.ChunkUploadService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户分片上传 HTTP 入口，按用户和 uploadId 接收分片、查询已接收分片，并返回合并是否完成及对象键.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/user/upload")
public class UploadControllerV1 {

    private final ChunkUploadService chunkUploadService;

    /**
     * 注入分片上传服务。
     *
     * @param chunkUploadService 分片保存、合并和状态查询服务
     */
    public UploadControllerV1(ChunkUploadService chunkUploadService) {
        this.chunkUploadService = chunkUploadService;
    }

    /**
     * 保存当前用户上传的一个文件分片。
     *
     * <p>缺少 Content-Type 时按 application/octet-stream 处理；分片服务返回是否完成、累计接收数量和对象键。
     *
     * @param userId 从 X-User-Id 获取的当前用户 ID
     * @param file 当前分片文件
     * @param uploadId 一次上传任务的稳定标识
     * @param chunkIndex 分片序号
     * @param totalChunks 分片总数
     * @param fileName 可选原始文件名
     * @return 当前分片处理结果
     */
    @PostMapping("/chunk")
    public Result<ChunkResultVo> chunk(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam(value = "fileName", required = false) String fileName) {
        String contentType =
                file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        ChunkUploadService.ChunkResult result =
                chunkUploadService.saveChunk(
                        userId, uploadId, chunkIndex, totalChunks, contentType, fileName, file::getInputStream);
        return Result.success(new ChunkResultVo(result.done(), result.received(), result.objectKey()));
    }

    /**
     * 查询一次分片上传任务已接收的分片。
     *
     * <p>当已接收分片数量等于 totalChunks 时，响应中的 done 为 true。
     *
     * @param userId 从 X-User-Id 获取的当前用户 ID
     * @param uploadId 上传任务标识
     * @param totalChunks 分片总数
     * @return 已接收分片序号和完成状态
     */
    @GetMapping("/status")
    public Result<ChunkStatusVo> status(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("totalChunks") int totalChunks) {
        List<Integer> received = chunkUploadService.receivedChunks(userId, uploadId, totalChunks);
        return Result.success(new ChunkStatusVo(uploadId, received, received.size() == totalChunks));
    }

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: 分片上传结果视图，返回是否完成、累计接收数量及完成后的对象键.
     * @Version: 1.0
     * @DateTime: 2026/10/16
     */
    public record ChunkResultVo(boolean done, int received, String objectKey) {}

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: 分片上传状态视图，返回上传标识、已接收分片序号及是否已收齐全部分片.
     * @Version: 1.0
     * @DateTime: 2026/10/16
     */
    public record ChunkStatusVo(String uploadId, List<Integer> receivedChunks, boolean done) {}
}
