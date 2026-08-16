package com.yirancrazy.minimall.goods.controller.v1;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UploadControllerV1 MockMvc 单元测试，验证商家端分片上传与断点状态查询的路由、参数绑定与 Result 包装。
 * @Version: 1.0
 * @DateTime: 2026/08/12
 **/
class UploadControllerV1Test {

    private MockMvc mockMvc;
    private ChunkUploadService chunkUploadService;

    @BeforeEach
    void setUp() {
        chunkUploadService = mock(ChunkUploadService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UploadControllerV1(chunkUploadService)).build();
    }

    @Test
    void chunk_returns_received_count_when_not_done() throws Exception {
        when(chunkUploadService.saveChunk(anyString(), anyInt(), anyInt(), anyString(), any(), any()))
            .thenReturn(new ChunkResult(false, 1, null));

        MockMultipartFile file = new MockMultipartFile("file", "a.png",
            "image/png", new byte[] {1, 2, 3});
        mockMvc.perform(multipart("/api/v1/merchant/upload/chunk")
                .file(file)
                .param("uploadId", "task-1")
                .param("chunkIndex", "0")
                .param("totalChunks", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.done").value(false))
            .andExpect(jsonPath("$.data.received").value(1));
        verify(chunkUploadService).saveChunk(eq("task-1"), eq(0), eq(3), eq("image/png"), any(), any());
    }

    @Test
    void chunk_returns_object_key_when_done() throws Exception {
        when(chunkUploadService.saveChunk(anyString(), anyInt(), anyInt(), anyString(), any(), any()))
            .thenReturn(new ChunkResult(true, 3, "final-key"));

        MockMultipartFile file = new MockMultipartFile("file", "a.png",
            "image/png", new byte[] {1, 2, 3});
        mockMvc.perform(multipart("/api/v1/merchant/upload/chunk")
                .file(file)
                .param("uploadId", "task-1")
                .param("chunkIndex", "2")
                .param("totalChunks", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.done").value(true))
            .andExpect(jsonPath("$.data.objectKey").value("final-key"));
    }

    @Test
    void status_returns_received_chunks() throws Exception {
        when(chunkUploadService.receivedChunks("task-1", 3))
            .thenReturn(List.of(0, 2));

        mockMvc.perform(get("/api/v1/merchant/upload/status")
                .param("uploadId", "task-1")
                .param("totalChunks", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.receivedChunks[0]").value(0))
            .andExpect(jsonPath("$.data.receivedChunks[1]").value(2))
            .andExpect(jsonPath("$.data.done").value(false));
        verify(chunkUploadService).receivedChunks("task-1", 3);
    }

    @Test
    void status_marks_done_when_all_received() throws Exception {
        when(chunkUploadService.receivedChunks("task-1", 2))
            .thenReturn(List.of(0, 1));

        mockMvc.perform(get("/api/v1/merchant/upload/status")
                .param("uploadId", "task-1")
                .param("totalChunks", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.done").value(true));
    }
}
