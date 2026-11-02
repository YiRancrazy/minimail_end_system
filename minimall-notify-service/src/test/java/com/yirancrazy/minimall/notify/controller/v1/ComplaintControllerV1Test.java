package com.yirancrazy.minimall.notify.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yirancrazy.minimall.notify.dto.ComplaintCreateDto;
import com.yirancrazy.minimall.notify.service.ComplaintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证投诉创建接口透传可信用户、幂等键并保持 Result 响应。
 * @Version: 1.0
 * @DateTime: 2026/11/2 9:16
 */
class ComplaintControllerV1Test {

    private ComplaintService complaintService;
    private MockMvc mockMvc;

    /**
     * 初始化投诉控制器单元测试夹具。
     */
    @BeforeEach
    void setUp() {
        complaintService = mock(ComplaintService.class);
        mockMvc =
                MockMvcBuilders.standaloneSetup(new ComplaintControllerV1(complaintService))
                        .build();
    }

    /** * 验证创建投诉返回首次生成的投诉主键. */
    @Test
    void testCreateReturnsComplaintId() throws Exception {
        ComplaintCreateDto dto = new ComplaintCreateDto();
        dto.setComplainantType(1);
        dto.setDefendantType(2);
        dto.setDefendantId(200L);
        dto.setComplaintType("QUALITY");
        dto.setTitle("商品质量问题");
        dto.setContent("收到商品与描述不符");
        when(complaintService.create(
                        eq(100L),
                        any(ComplaintCreateDto.class),
                        eq("complaint:create:100:req-1")))
                .thenReturn(9001L);

        mockMvc
                .perform(
                        post("/api/v1/platform/complaints")
                                .header("X-User-Id", 100L)
                                .header("X-Idempotency-Key", "req-1")
                                .requestAttr(
                                        "idempotencyKey",
                                        "complaint:create:100:req-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"complainantType\":1,"
                                                + "\"defendantType\":2,"
                                                + "\"defendantId\":200,"
                                                + "\"complaintType\":\"QUALITY\","
                                                + "\"title\":\"测试投诉\","
                                                + "\"content\":\"测试内容\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(9001));

        verify(complaintService)
                .create(
                        eq(100L),
                        any(ComplaintCreateDto.class),
                        eq("complaint:create:100:req-1"));
    }

    /** * 验证缺少客户端幂等键时请求失败. */
    @Test
    void testCreateWithoutIdempotencyHeaderFails() throws Exception {
        mockMvc
                .perform(
                        post("/api/v1/platform/complaints")
                                .header("X-User-Id", 100L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
