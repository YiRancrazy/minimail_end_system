package com.yirancrazy.minimall.pay.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yirancrazy.minimall.api.dto.pay.RefundCreateDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证支付、回调和退款 DTO 的金额精度约束。
 * 直接使用 Jakarta Validator 检查整数位和小数位边界，不启动 Spring 容器。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class AmountDigitsValidationTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  /** 验证创建支付金额：输入两位小数，预期校验无违规. */
  @Test
  void testPayCreateDtoAcceptsTwoDecimals() {
    PayCreateDto dto = new PayCreateDto("ORDER1", new BigDecimal("99.99"), null);
    assertTrue(validator.validate(dto).isEmpty());
  }

  /** 验证创建支付金额：输入三位小数，预期仅金额字段违规. */
  @Test
  void testPayCreateDtoRejectsThreeDecimals() {
    PayCreateDto dto = new PayCreateDto("ORDER1", new BigDecimal("99.991"), null);
    assertEquals("amount", onlyViolation(validator.validate(dto)));
  }

  /** 验证创建支付金额：整数部分超过十位，预期仅金额字段违规. */
  @Test
  void testPayCreateDtoRejectsAmountBeyondIntegerDigits() {
    PayCreateDto dto = new PayCreateDto("ORDER1", new BigDecimal("12345678901.00"), null);
    assertEquals("amount", onlyViolation(validator.validate(dto)));
  }

  /** 验证支付回调金额：输入三位小数，预期仅回调金额字段违规. */
  @Test
  void testCallbackDtoRejectsThreeDecimals() {
    PayCallbackDto dto =
        new PayCallbackDto("PAY1", "TRADE1", true, "SUCCESS", new BigDecimal("10.005"));
    assertEquals("totalAmount", onlyViolation(validator.validate(dto)));
  }

  /** 验证退款金额：输入三位小数，预期仅退款金额字段违规. */
  @Test
  void testRefundCreateDtoRejectsThreeDecimals() {
    RefundCreateDto dto =
        new RefundCreateDto(1L, new BigDecimal("10.005"), "reason", "REFUND-REQ-1");
    assertEquals("amount", onlyViolation(validator.validate(dto)));
  }

  /** 验证退款金额：输入两位小数，预期校验无违规. */
  @Test
  void testRefundCreateDtoAcceptsTwoDecimals() {
    RefundCreateDto dto =
        new RefundCreateDto(1L, new BigDecimal("10.00"), "reason", "REFUND-REQ-1");
    assertTrue(validator.validate(dto).isEmpty());
  }

  private String onlyViolation(Set<? extends ConstraintViolation<?>> violations) {
    assertEquals(1, violations.size());
    return violations.iterator().next().getPropertyPath().toString();
  }
}
