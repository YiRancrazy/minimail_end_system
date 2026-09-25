package com.yirancrazy.minimall.pay.dto;

import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 金额 DTO 校验测试，校验 @Digits(integer=10, fraction=2) 拒绝超过两位小数的金额。
 * @Version: 1.0
 * @DateTime: 2026/09/23
 **/
class AmountDigitsValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void pay_create_dto_accepts_two_decimals() {
        PayCreateDTO dto = new PayCreateDTO("ORDER1", new BigDecimal("99.99"), null);
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void pay_create_dto_rejects_three_decimals() {
        PayCreateDTO dto = new PayCreateDTO("ORDER1", new BigDecimal("99.991"), null);
        assertEquals("amount", onlyViolation(validator.validate(dto)));
    }

    @Test
    void pay_create_dto_rejects_amount_beyond_integer_digits() {
        PayCreateDTO dto = new PayCreateDTO("ORDER1", new BigDecimal("12345678901.00"), null);
        assertEquals("amount", onlyViolation(validator.validate(dto)));
    }

    @Test
    void callback_dto_rejects_three_decimals() {
        PayCallbackDTO dto = new PayCallbackDTO("PAY1", "TRADE1", true, "SUCCESS",
            new BigDecimal("10.005"));
        assertEquals("totalAmount", onlyViolation(validator.validate(dto)));
    }

    @Test
    void refund_create_dto_rejects_three_decimals() {
        RefundCreateDTO dto = new RefundCreateDTO(
            1L, new BigDecimal("10.005"), "reason", "REFUND-REQ-1");
        assertEquals("amount", onlyViolation(validator.validate(dto)));
    }

    @Test
    void refund_create_dto_accepts_two_decimals() {
        RefundCreateDTO dto = new RefundCreateDTO(
            1L, new BigDecimal("10.00"), "reason", "REFUND-REQ-1");
        assertTrue(validator.validate(dto).isEmpty());
    }

    private String onlyViolation(Set<? extends ConstraintViolation<?>> violations) {
        assertEquals(1, violations.size());
        return violations.iterator().next().getPropertyPath().toString();
    }
}
