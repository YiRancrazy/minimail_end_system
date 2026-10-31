package com.yirancrazy.minimall.order.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证订单结算请求 DTO 的 Jakarta Validation 契约。
 *   业务边界：直接构造空 DTO 并运行默认 Validator，不依赖 Spring 或数据库。
 *   使用场景：防止 checkoutSource 缺失时绕过必填校验。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
class OrderCheckoutDtoTest {

  /** 验证结算来源是必需. */
  @Test
  void testCheckoutSourceIsRequired() {
    OrderCheckoutDto dto = new OrderCheckoutDto();
    Set<ConstraintViolation<OrderCheckoutDto>> violations =
        Validation.buildDefaultValidatorFactory().getValidator().validate(dto);

    assertTrue(
        violations.stream().anyMatch(v -> "checkoutSource".equals(v.getPropertyPath().toString())));
  }
}
