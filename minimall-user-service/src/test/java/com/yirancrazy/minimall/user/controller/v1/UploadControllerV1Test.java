package com.yirancrazy.minimall.user.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yirancrazy.minimall.common.service.ChunkUploadService;
import com.yirancrazy.minimall.common.service.ChunkUploadService.ChunkResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户分片上传控制器单元测试，使用 MockMvc 和 ChunkUploadService 替身验证分片响应与状态查询协议.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class UploadControllerV1Test {

  private MockMvc mockMvc;
  private ChunkUploadService chunkUploadService;

  /**
   * 初始化分片上传服务替身和 MockMvc。
   */
  @BeforeEach
  void setUp() {
    chunkUploadService = mock(ChunkUploadService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new UploadControllerV1(chunkUploadService)).build();
  }

  /** 验证分片未完成：服务返回 1/3，上传第 0 片，预期 00000、done=false、received=1. */
  @Test
  void testChunkReturnsReceivedCountWhenNotDone() throws Exception {
    when(chunkUploadService.saveChunk(
            anyLong(),
            anyString(),
            anyInt(),
            anyInt(),
            anyString(),
            any(),
            any(ChunkUploadService.ChunkInputStreamSupplier.class)))
        .thenReturn(new ChunkResult(false, 1, null));

    MockMultipartFile file =
        new MockMultipartFile("file", "a.png", "image/png", new byte[] {1, 2, 3});
    mockMvc
        .perform(
            multipart("/api/v1/user/upload/chunk")
                .file(file)
                .header("X-User-Id", "42")
                .param("uploadId", "task-1")
                .param("chunkIndex", "0")
                .param("totalChunks", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data.done").value(false))
        .andExpect(jsonPath("$.data.received").value(1));
    verify(chunkUploadService)
        .saveChunk(
            eq(42L),
            eq("task-1"),
            eq(0),
            eq(3),
            eq("image/png"),
            any(),
            any(ChunkUploadService.ChunkInputStreamSupplier.class));
  }

  /** 验证末片完成：服务返回 3/3 和 final-key，上传第 2 片，预期 done=true、objectKey=final-key. */
  @Test
  void testChunkReturnsObjectKeyWhenDone() throws Exception {
    when(chunkUploadService.saveChunk(
            anyLong(),
            anyString(),
            anyInt(),
            anyInt(),
            anyString(),
            any(),
            any(ChunkUploadService.ChunkInputStreamSupplier.class)))
        .thenReturn(new ChunkResult(true, 3, "final-key"));

    MockMultipartFile file =
        new MockMultipartFile("file", "a.png", "image/png", new byte[] {1, 2, 3});
    mockMvc
        .perform(
            multipart("/api/v1/user/upload/chunk")
                .file(file)
                .header("X-User-Id", "42")
                .param("uploadId", "task-1")
                .param("chunkIndex", "2")
                .param("totalChunks", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.done").value(true))
        .andExpect(jsonPath("$.data.objectKey").value("final-key"));
  }

  /** 验证状态查询：3 片任务已收 0、2，预期 00000、receivedChunks=[0,2]、done=false. */
  @Test
  void testStatusReturnsReceivedChunks() throws Exception {
    when(chunkUploadService.receivedChunks(42L, "task-1", 3)).thenReturn(List.of(0, 2));

    mockMvc
        .perform(
            get("/api/v1/user/upload/status")
                .header("X-User-Id", "42")
                .param("uploadId", "task-1")
                .param("totalChunks", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data.receivedChunks[0]").value(0))
        .andExpect(jsonPath("$.data.receivedChunks[1]").value(2))
        .andExpect(jsonPath("$.data.done").value(false));
    verify(chunkUploadService).receivedChunks(42L, "task-1", 3);
  }

  /** 验证状态完成：2 片任务已收 0、1，调用 status，预期 done=true. */
  @Test
  void testStatusMarksDoneWhenAllReceived() throws Exception {
    when(chunkUploadService.receivedChunks(42L, "task-1", 2)).thenReturn(List.of(0, 1));

    mockMvc
        .perform(
            get("/api/v1/user/upload/status")
                .header("X-User-Id", "42")
                .param("uploadId", "task-1")
                .param("totalChunks", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.done").value(true));
  }
}
