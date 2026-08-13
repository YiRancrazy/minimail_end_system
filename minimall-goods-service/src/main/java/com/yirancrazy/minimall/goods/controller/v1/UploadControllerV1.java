package com.yirancrazy.minimall.goods.controller.v1;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.yirancrazy.minimall.common.constant.UploadCodeEnum;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.service.ChunkUploadService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家端文件上传控制器，提供分片上传与断点查询（商品图等）。
 * @Version: 2.0
 * @DateTime: 2026/08/12
 **/
@RestController
@RequestMapping("/api/v1/merchant/upload")
public class UploadControllerV1 {

    private final ChunkUploadService chunkUploadService;

    public UploadControllerV1(ChunkUploadService chunkUploadService) {
        this.chunkUploadService = chunkUploadService;
    }

    /**
     * 上传单个分片；分片全部到齐后后端合并并存储 MinIO。
     * @param file 分片文件
     * @param uploadId 上传任务 ID
     * @param chunkIndex 分片序号（0 起）
     * @param totalChunks 总分片数
     * @return 分片结果；done=true 时 data.objectKey 为最终文件 key
     */
    @PostMapping("/chunk")
    public Result<ChunkResultVO> chunk(@RequestParam("file") MultipartFile file,
                                       @RequestParam("uploadId") String uploadId,
                                       @RequestParam("chunkIndex") int chunkIndex,
                                       @RequestParam("totalChunks") int totalChunks) {
        ChunkUploadService.ChunkResult result;
        try (InputStream in = file.getInputStream()) {
            result = chunkUploadService.saveChunk(
                uploadId, chunkIndex, totalChunks,
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                in);
        }
        catch (IOException e) {
            throw new BizException(UploadCodeEnum.CHUNK_UPLOAD_FAIL);
        }
        return Result.success(new ChunkResultVO(result.done(), result.received(), result.objectKey()));
    }

    /**
     * 查询已接收分片序号，用于断点续传。
     * @param uploadId 上传任务 ID
     * @param totalChunks 总分片数
     * @return 已接收分片序号列表
     */
    @GetMapping("/status")
    public Result<ChunkStatusVO> status(@RequestParam("uploadId") String uploadId,
                                        @RequestParam("totalChunks") int totalChunks) {
        List<Integer> received = chunkUploadService.receivedChunks(uploadId, totalChunks);
        return Result.success(new ChunkStatusVO(uploadId, received, received.size() == totalChunks));
    }

    /** 分片上传结果 VO */
    public record ChunkResultVO(boolean done, int received, String objectKey) {
    }

    /** 分片状态 VO */
    public record ChunkStatusVO(String uploadId, List<Integer> receivedChunks, boolean done) {
    }
}
