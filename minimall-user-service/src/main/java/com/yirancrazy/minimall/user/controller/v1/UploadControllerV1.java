package com.yirancrazy.minimall.user.controller.v1;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.service.ChunkUploadService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UploadControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/user/upload")
public class UploadControllerV1 {

    private final ChunkUploadService chunkUploadService;

    /**
     * 构造 UploadControllerV1 实例。
     * @param chunkUploadService chunkUploadService 参数
     */
    public UploadControllerV1(ChunkUploadService chunkUploadService) {
        this.chunkUploadService = chunkUploadService;
    }


    /**
     * 执行 chunk 方法。
     * @param userId userId 参数
     * @param file file 参数
     * @param uploadId uploadId 参数
     * @param chunkIndex chunkIndex 参数
     * @param totalChunks totalChunks 参数
     * @param fileName fileName 参数
     * @return 统一响应结果
     */
    @PostMapping("/chunk")
    public Result<ChunkResultVO> chunk(@RequestHeader("X-User-Id") Long userId,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam("uploadId") String uploadId,
                                       @RequestParam("chunkIndex") int chunkIndex,
                                       @RequestParam("totalChunks") int totalChunks,
                                       @RequestParam(value = "fileName", required = false) String fileName) {
        String contentType = file.getContentType() != null
            ? file.getContentType() : "application/octet-stream";
        ChunkUploadService.ChunkResult result = chunkUploadService.saveChunk(
            userId, uploadId, chunkIndex, totalChunks, contentType, fileName, file::getInputStream);
        return Result.success(new ChunkResultVO(result.done(), result.received(), result.objectKey()));
    }


    /**
     * 执行 status 方法。
     * @param userId userId 参数
     * @param uploadId uploadId 参数
     * @param totalChunks totalChunks 参数
     * @return 统一响应结果
     */
    @GetMapping("/status")
    public Result<ChunkStatusVO> status(@RequestHeader("X-User-Id") Long userId,
                                        @RequestParam("uploadId") String uploadId,
                                        @RequestParam("totalChunks") int totalChunks) {
        List<Integer> received = chunkUploadService.receivedChunks(userId, uploadId, totalChunks);
        return Result.success(new ChunkStatusVO(uploadId, received, received.size() == totalChunks));
    }


    /**
     * @Author: yirancrazy@gmail.com
     * @Description: ChunkResultVO 视图对象。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    public record ChunkResultVO(boolean done, int received, String objectKey) {
    }


    /**
     * @Author: yirancrazy@gmail.com
     * @Description: ChunkStatusVO 视图对象。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    public record ChunkStatusVO(String uploadId, List<Integer> receivedChunks, boolean done) {
    }
}
