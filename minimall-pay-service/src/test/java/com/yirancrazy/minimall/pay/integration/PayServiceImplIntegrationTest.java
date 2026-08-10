package com.yirancrazy.minimall.pay.integration;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.dto.WithdrawApplyDTO;
import com.yirancrazy.minimall.pay.entity.MerchantWithdrawPO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.gateway.AlipayGateway;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.WithdrawVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayService 集成测试，使用 H2 内存库 + Mock 外部网关 / Feign。
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@SpringBootTest
@ActiveProfiles("test")
class PayServiceImplIntegrationTest {

    @Autowired
    private PayService payService;

    @MockBean
    private AlipayGateway alipayGateway;

    @MockBean
    private OrderFeignClient orderFeignClient;

    /**
     * 测试上下文：注册 MetaObjectHandler 解决 BasePO.isDeleted 自动填充。
     * 默认 service.save 不会显式 setIsDeleted，需要在 INSERT 前填 0。
     */
    @org.springframework.boot.test.context.TestConfiguration
    static class TestMetaObjectConfig {

        @org.springframework.context.annotation.Bean
        com.baomidou.mybatisplus.core.handlers.MetaObjectHandler metaObjectHandler() {
            return new com.baomidou.mybatisplus.core.handlers.MetaObjectHandler() {
                @Override
                public void insertFill(org.apache.ibatis.reflection.MetaObject metaObject) {
                    this.strictInsertFill(metaObject, "createTime", java.time.LocalDateTime.class,
                        java.time.LocalDateTime.now());
                    this.strictInsertFill(metaObject, "updateTime", java.time.LocalDateTime.class,
                        java.time.LocalDateTime.now());
                    this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
                    this.strictInsertFill(metaObject, "version", Integer.class, 0);
                }

                @Override
                public void updateFill(org.apache.ibatis.reflection.MetaObject metaObject) {
                    this.strictUpdateFill(metaObject, "updateTime", java.time.LocalDateTime.class,
                        java.time.LocalDateTime.now());
                }
            };
        }
    }

    @BeforeEach
    void setUp() {
        lenient().when(alipayGateway.createPayment(anyString(), any(), anyString(), anyString()))
            .thenReturn("http://pay.url/test");
        lenient().when(alipayGateway.refund(anyString(), anyString(), any(), anyString()))
            .thenReturn("REFUND_TRADE_TEST");
    }

    /**
     * 端到端：createPayment 写入 PENDING 行后 getByOrderNo 可见。
     */
    @Test
    void createPayment_persists_and_getByOrderNo_returns_it() {
        Long payId = payService.createPayment("ORD-INT-1", 100L, 200L, new BigDecimal("99.99"), null);
        assertNotNull(payId);

        PayTransactionPO po = payService.getByOrderNo("ORD-INT-1");
        assertNotNull(po);
        assertEquals(1, po.getStatus());
        assertEquals(0, new BigDecimal("99.99").compareTo(po.getAmount()));
    }

    /**
     * 端到端：createPayment 显式 channel=2（WECHAT）应抛 PAY_CHANNEL_UNSUPPORTED。
     */
    @Test
    void createPayment_unsupported_channel_throws() {
        assertThrows(BizException.class,
            () -> payService.createPayment("ORD-INT-2", 100L, 200L, new BigDecimal("10.00"), 2));
    }

    /**
     * 端到端：applyWithdraw 持久化一条 PENDING 提现单并能 pageWithdraw 拉回。
     */
    @Test
    void applyWithdraw_then_pageWithdraw_returns_record() {
        WithdrawApplyDTO dto = new WithdrawApplyDTO(new BigDecimal("1000.00"), "INTEG");
        WithdrawVO vo = payService.applyWithdraw(7L, dto);
        assertNotNull(vo.getWithdrawNo());
        assertEquals(7L, vo.getMerchantId());
        assertEquals(1, vo.getStatus());

        PayPageDTO page = new PayPageDTO();
        page.setLimit(10);
        CursorPageVO<MerchantWithdrawPO> result = payService.pageWithdraw(7L, page);
        assertNotNull(result);
        assertTrue(result.getRecords().size() >= 1);
        assertEquals(7L, result.getRecords().get(0).getMerchantId());
    }

    /**
     * 端到端：getPaymentParams 返回的 VO 字段与持久化一致。
     */
    @Test
    void getPaymentParams_returns_persisted_fields() {
        payService.createPayment("ORD-INT-3", 1L, 2L, new BigDecimal("50.00"), null);
        PayTransactionPO po = payService.getByOrderNo("ORD-INT-3");

        com.yirancrazy.minimall.pay.vo.PaymentParamsVO params = payService.getPaymentParams(po.getPaymentNo());
        assertNotNull(params);
        assertEquals(po.getPaymentNo(), params.getPaymentNo());
        assertEquals(0, new BigDecimal("50.00").compareTo(params.getAmount()));
        assertEquals("CNY", params.getCurrency());
    }

    /**
     * 端到端：getByOrderNo 不存在时抛 PAY_NOT_FOUND。
     */
    @Test
    void getByOrderNo_missing_throws_biz() {
        assertThrows(BizException.class, () -> payService.getByOrderNo("ORD-INT-NOT-EXIST"));
    }
}
