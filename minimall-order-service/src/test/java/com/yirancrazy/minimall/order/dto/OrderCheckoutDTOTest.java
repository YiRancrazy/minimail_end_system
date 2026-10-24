package com.yirancrazy.minimall.order.dto;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单结算请求契约测试。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
class OrderCheckoutDTOTest {

    @Test
    void checkoutSource_isRequired() {
        OrderCheckoutDTO dto = new OrderCheckoutDTO();
        Set<ConstraintViolation<OrderCheckoutDTO>> violations =
            Validation.buildDefaultValidatorFactory().getValidator().validate(dto);

        assertTrue(violations.stream()
            .anyMatch(v -> "checkoutSource".equals(v.getPropertyPath().toString())));
    }
}
