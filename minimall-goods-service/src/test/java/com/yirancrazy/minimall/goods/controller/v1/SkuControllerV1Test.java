package com.yirancrazy.minimall.goods.controller.v1;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.service.SkuService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SkuControllerV1Test {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    SkuService skuService;

    @Test
    void create_then_get() {
        SkuCreateDTO dto = new SkuCreateDTO();
        dto.setSpuId(1L);
        dto.setSkuName("TestSKU");
        dto.setPrice(new BigDecimal("99.90"));
        dto.setStock(100);
        Long id = skuService.create(dto);

        ResponseEntity<Result> r = rest.getForEntity(
            "http://localhost:" + port + "/api/v1/goods/sku/" + id, Result.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("00000", r.getBody().getCode());
    }
}