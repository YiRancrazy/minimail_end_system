package com.yirancrazy.minimall.pay.schedule;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.pay.service.PayService;


/**
 * @Author: YiRanCrazy@gmail.com
 * @Description: 支付定时任务 Redis 分布式锁行为测试。
 * @Version: 1.0
 * @DateTime: 2026/10/21 20:04
 **/
@SpringBootTest(
    classes = PayScheduleHandlerTest.MinimalContext.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        // 开启 PayScheduleHandler Bean。
        "minimall.schedule.enabled=true",
        // 测试仅手工调用方法，不开启调度基础设施。
        "minimall.xxl-job.enabled=false"
    }
)
@SuppressWarnings("unchecked")
public class PayScheduleHandlerTest {

    private static final String PAID_KEY = "pay:schedule:scanPaidButOrderPending";
    private static final String UNNOTIFIED_KEY = "pay:schedule:scanUnnotifiedRefunds";
    private static final String PENDING_KEY = "pay:schedule:scanPendingRefunds";

    // 扫描业务依赖，使用 Mock 隔离真实支付业务。
    @MockBean
    private PayService payService;

    // Redis 客户端使用 Mock，不连接真实 Redis。
    @MockBean
    private StringRedisTemplate redisTemplate;

    // 模拟 StringRedisTemplate.opsForValue()。
    @MockBean
    private ValueOperations<String, String> valueOperations;

    // 被测对象必须是真实 Bean，不能使用 @MockBean。
    @Autowired
    private PayScheduleHandler handler;

    /**
     * 初始化 Redis Mock 默认行为。
     */
    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(true);
    }


    /**
     * 最小测试上下文，仅加载被测定时任务，避免数据库和外部服务干扰。
     */
    @Configuration(proxyBeanMethods = false)
    @Import(PayScheduleHandler.class)
    static class MinimalContext {
    }


    /**
     * 验证三个任务抢锁成功后均执行扫描并释放锁。
     */
    @Test
    void shouldExecuteAllScansWhenLocksAcquired() {
        invokeAll();

        verify(payService).scanPaidButOrderPending();
        verify(payService).scanUnnotifiedRefunds();
        verify(payService).scanPendingRefunds();
        verify(redisTemplate, times(3))
            .execute(any(RedisScript.class), anyList(), anyString());
    }


    /**
     * 验证锁已被占用时，三个任务均跳过业务扫描。
     */
    @Test
    void shouldSkipAllScansWhenLocksHeld() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
            .thenReturn(false);

        invokeAll();

        verifyNoInteractions(payService);
        verify(redisTemplate, never())
            .execute(any(RedisScript.class), anyList(), anyString());
    }


    /**
     * 验证三个任务使用互不相同的锁键。
     */
    @Test
    void shouldUseDistinctLockKeys() {
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        when(valueOperations.setIfAbsent(
            keyCaptor.capture(), anyString(), any(Duration.class)))
            .thenReturn(true);

        invokeAll();

        assertEquals(
            Set.of(PAID_KEY, UNNOTIFIED_KEY, PENDING_KEY),
            new HashSet<>(keyCaptor.getAllValues())
        );
    }


    /**
     * 验证释放锁时使用加锁 token，并校验当前 TTL。
     */
    @Test
    void shouldReleaseWithSameTokenAndExpectedTtl() {
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        when(valueOperations.setIfAbsent(
            eq(PENDING_KEY), tokenCaptor.capture(), eq(Duration.ofMinutes(10))))
            .thenReturn(true);

        handler.scanPendingRefunds();

        verify(redisTemplate).execute(
            any(RedisScript.class),
            eq(List.of(PENDING_KEY)),
            eq(tokenCaptor.getValue())
        );
    }


    /**
     * 验证扫描异常时仍执行解锁，并保留原异常。
     */
    @Test
    void shouldReleaseWhenScanThrows() {
        when(payService.scanPendingRefunds())
            .thenThrow(new IllegalStateException("scan failed"));

        assertThrows(IllegalStateException.class, handler::scanPendingRefunds);

        verify(redisTemplate).execute(
            any(RedisScript.class),
            eq(List.of(PENDING_KEY)),
            anyString()
        );
    }


    /**
     * 手工调用三个扫描任务。
     */
    private void invokeAll() {
        handler.scanPaidButOrderPending();
        handler.scanUnnotifiedRefunds();
        handler.scanPendingRefunds();
    }
}
