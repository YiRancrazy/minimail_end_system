package com.yirancrazy.minimall.merchant.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.service.ShopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ShopControllerV1Test {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ShopService shopService;

    @Test
    void create_then_get() {
        ShopPO s = new ShopPO();
        s.setShopName("MyShop");
        s.setLicenseNo("L123");
        s.setStatus("ACTIVE");
        Long id = shopService.create(s);

        ResponseEntity<Result> r = rest.getForEntity(
            "http://localhost:" + port + "/api/v1/merchant/shop/" + id, Result.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("00000", r.getBody().getCode());
    }
}