package com.yirancrazy.minimall.notify.handle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.common.handle.IdempotencyContext;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.entity.ComplaintPo;
import com.yirancrazy.minimall.notify.manager.ComplaintManager;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证投诉创建重放处理器按幂等键返回首次投诉。
 * @Version: 1.0
 * @DateTime: 2026/11/2 9:16
 */
class ComplaintCreateReplayHandlerTest {

    /** * 验证已有投诉返回同一主键. */
    @Test
    void testExistingComplaintReturnsSameId() throws Exception {
        ComplaintManager complaintManager = mock(ComplaintManager.class);
        Method method =
                ComplaintCreateReplayHandlerTest.class.getDeclaredMethod(
                        "testExistingComplaintReturnsSameId");
        ComplaintCreateReplayHandler handler =
                new ComplaintCreateReplayHandler(complaintManager);
        ComplaintPo complaint = new ComplaintPo();
        complaint.setId(9001L);
        when(complaintManager.findByIdempotencyKey("complaint:create:100:req-1"))
                .thenReturn(complaint);

        Result<Long> result =
                handler.onDuplicate(
                        new IdempotencyContext(
                                method,
                                new Object[0],
                                "complaint:create:100:req-1"));

        assertEquals(9001L, result.getData());
    }

    /** * 验证投诉尚未可见时返回 null. */
    @Test
    void testMissingComplaintReturnsNull() throws Exception {
        ComplaintManager complaintManager = mock(ComplaintManager.class);
        Method method =
                ComplaintCreateReplayHandlerTest.class.getDeclaredMethod(
                        "testMissingComplaintReturnsNull");
        ComplaintCreateReplayHandler handler =
                new ComplaintCreateReplayHandler(complaintManager);
        when(complaintManager.findByIdempotencyKey("complaint:create:100:req-1"))
                .thenReturn(null);

        assertNull(
                handler.onDuplicate(
                        new IdempotencyContext(
                                method,
                                new Object[0],
                                "complaint:create:100:req-1")));
    }
}
