package com.yirancrazy.minimall.goods.controller.v1;

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
 * @Description: 面向商家端接收分片上传并查询上传进度，最终由分片服务合并为 MinIO 对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/merchant/upload")
public class UploadControllerV1 {

  private final ChunkUploadService chunkUploadService;

  /**
   * 注入分片保存、合并和进度查询服务，构造 UploadControllerV1 实例。
   *
   * @param chunkUploadService 分片保存、合并和进度查询服务
   */
  public UploadControllerV1(ChunkUploadService chunkUploadService) {
    this.chunkUploadService = chunkUploadService;
  }

  /**
   * 接收单个分片并委托分片服务保存或合并，返回完成状态、接收数和对象键。
   *
   * @param userId 当前登录用户 ID，用于隔离上传任务
   * @param file 当前分片文件内容
   * @param uploadId 分片上传任务标识
   * @param chunkIndex 从零开始的分片序号
   * @param totalChunks 上传任务预计分片总数
   * @param fileName 原始文件名；为空时由上传服务按任务规则处理
   * @return 当前分片处理结果；完成时包含合并后的 MinIO 对象键
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
   * 查询指定上传任务已接收的分片序号，并根据数量判断是否完成。
   *
   * @param userId 当前登录用户 ID，用于隔离上传任务
   * @param uploadId 分片上传任务标识
   * @param totalChunks 上传任务预计分片总数
   * @return 上传任务进度及完成标记
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
   * @Description: 承载单个分片上传后的完成状态、已接收数量及合并对象键。
   * @Version: 1.0
   * @DateTime: 2026/10/16
   */
  public record ChunkResultVo(boolean done, int received, String objectKey) {}

  /**
   * @Author: yirancrazy@gmail.com
   * @Description: 承载分片上传任务已接收的序号列表和整体完成状态。
   * @Version: 1.0
   * @DateTime: 2026/10/16
   */
  public record ChunkStatusVo(String uploadId, List<Integer> receivedChunks, boolean done) {}
}
