package com.yirancrazy.minimall.order.manager.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.mapper.OrderMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证支付超时扫描的查询时间边界。
 * @Version: 1.0
 * @DateTime: 2026/10/25 18:50
 **/
class OrderManagerImplExpirationBoundaryTest {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 10, 25, 12, 0, 0);

    private OrderMapper orderMapper;

    private OrderManagerImpl orderManager;

    private MockedStatic<LocalDateTime> localDateTimeMock;

    /**
     * 初始化 Lambda 查询所需的实体元数据。
     */
    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), OrderPO.class);
    }

    /**
     * 初始化被测 Manager 并固定当前时间。
     */
    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        when(orderMapper.selectList(any())).thenReturn(Collections.emptyList());
        orderManager = new OrderManagerImpl(orderMapper);
        localDateTimeMock = Mockito.mockStatic(LocalDateTime.class, Mockito.CALLS_REAL_METHODS);
        localDateTimeMock.when(LocalDateTime::now).thenReturn(FIXED_NOW);
    }

    /**
     * 恢复 LocalDateTime 的真实静态行为。
     */
    @AfterEach
    void tearDown() {
        localDateTimeMock.close();
    }

    /**
     * 建单后 29 分 59 秒时，订单不应满足支付过期条件。
     */
    @Test
    void queryBoundary_excludesOrderBeforeThirtyMinutes() {
        LambdaQueryWrapper<OrderPO> wrapper = queryWrapper();
        LocalDateTime threshold = singleTimeParameter(wrapper);

        assertTrue(wrapper.getSqlSegment().contains("pay_expire_at <="));
        assertEquals(FIXED_NOW, threshold);
        assertFalse(matchesInclusiveUpperBound(FIXED_NOW.plusSeconds(1), threshold));
    }

    /**
     * 建单后刚好 30 分钟时，订单应满足支付过期条件。
     */
    @Test
    void queryBoundary_includesOrderAtThirtyMinutes() {
        LambdaQueryWrapper<OrderPO> wrapper = queryWrapper();
        LocalDateTime threshold = singleTimeParameter(wrapper);

        assertTrue(wrapper.getSqlSegment().contains("pay_expire_at <="));
        assertEquals(FIXED_NOW, threshold);
        assertTrue(matchesInclusiveUpperBound(FIXED_NOW, threshold));
    }

    /**
     * 建单后 31 分钟时，订单应满足支付过期条件。
     */
    @Test
    void queryBoundary_includesOrderAfterThirtyMinutes() {
        LambdaQueryWrapper<OrderPO> wrapper = queryWrapper();
        LocalDateTime threshold = singleTimeParameter(wrapper);

        assertTrue(wrapper.getSqlSegment().contains("pay_expire_at <="));
        assertEquals(FIXED_NOW, threshold);
        assertTrue(matchesInclusiveUpperBound(FIXED_NOW.minusMinutes(1), threshold));
    }

    /**
     * 空支付过期时间应使用创建时间加 30 分钟作为兜底边界。
     */
    @Test
    @Disabled("O-05 null fallback is temporarily ignored")
    void queryBoundary_usesCreateTimeFallbackWhenPayExpireAtIsNull() {
        LambdaQueryWrapper<OrderPO> wrapper = queryWrapper();
        Map<String, Object> parameters = wrapper.getParamNameValuePairs();
        LocalDateTime fallbackThreshold = FIXED_NOW.minusMinutes(30);

        assertTrue(wrapper.getSqlSegment().contains("create_time <="));
        assertTrue(parameters.containsValue(fallbackThreshold));
        assertFalse(matchesInclusiveUpperBound(FIXED_NOW.minusMinutes(29).minusSeconds(59), fallbackThreshold));
        assertTrue(matchesInclusiveUpperBound(FIXED_NOW.minusMinutes(30).minusSeconds(1), fallbackThreshold));
    }

    /**
     * 捕获 Manager 实际提交给 Mapper 的查询条件。
     * @return 超时订单查询条件
     */
    private LambdaQueryWrapper<OrderPO> queryWrapper() {
        orderManager.listExpiredPendingOrders(OrderStatusEnum.PENDING.intCode(), 0L, 100);
        ArgumentCaptor<Wrapper<OrderPO>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(orderMapper).selectList(captor.capture());
        LambdaQueryWrapper<OrderPO> wrapper = (LambdaQueryWrapper<OrderPO>) captor.getValue();
        wrapper.getSqlSegment();
        return wrapper;
    }

    /**
     * 获取唯一的时间边界参数。
     * @param wrapper 查询条件
     * @return 时间边界参数
     */
    private LocalDateTime singleTimeParameter(LambdaQueryWrapper<OrderPO> wrapper) {
        return wrapper.getParamNameValuePairs().values().stream()
            .filter(LocalDateTime.class::isInstance)
            .map(LocalDateTime.class::cast)
            .findFirst()
            .orElseThrow();
    }

    /**
     * 判断候选时间是否满足包含等值点的时间上界。
     * @param candidate 候选时间
     * @param threshold 时间上界
     * @return 候选时间不晚于上界时返回 true
     */
    private boolean matchesInclusiveUpperBound(LocalDateTime candidate, LocalDateTime threshold) {
        return !candidate.isAfter(threshold);
    }
}
