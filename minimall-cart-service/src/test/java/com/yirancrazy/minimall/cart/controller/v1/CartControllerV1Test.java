package com.yirancrazy.minimall.cart.controller.v1;

import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.cart.service.CartService;
import com.yirancrazy.minimall.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CartControllerV1Test {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    CartService cartService;

    @Test
    void add_then_list() {
        CartItemPO item = new CartItemPO();
        item.setUserId(1L);
        item.setSkuId(100L);
        item.setQuantity(2);
        Long id = cartService.add(item);

        ResponseEntity<Result> r = rest.getForEntity(
            "http://localhost:" + port + "/api/v1/cart?userId=1", Result.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("00000", r.getBody().getCode());
    }
}