package com.yirancrazy.minimall.stock.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.stock.service.StockService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockControllerV1 的单元测试类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StockControllerV1Test {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @MockBean
    StockService stockService;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/internal/stock/reserve";
        when(stockService.reserve(any(Long.class), any(Integer.class))).thenReturn(true);
    }

    @Test
    void reserve_should_success_with_valid_params() {
        StockReserveDTO dto = new StockReserveDTO(1001L, 10);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<StockReserveDTO> request = new HttpEntity<>(dto, headers);

        ResponseEntity<Result> response = rest.postForEntity(baseUrl, request, Result.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("00000", response.getBody().getCode());
    }

    @Test
    void reserve_should_fail_when_skuId_is_null() {
        StockReserveDTO dto = new StockReserveDTO(null, 10);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<StockReserveDTO> request = new HttpEntity<>(dto, headers);

        ResponseEntity<Result> response = rest.postForEntity(baseUrl, request, Result.class);
        System.out.println("Response: " + response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("20001", response.getBody().getCode());
    }

    @Test
    void reserve_should_fail_when_quantity_is_zero() {
        StockReserveDTO dto = new StockReserveDTO(1001L, 0);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<StockReserveDTO> request = new HttpEntity<>(dto, headers);

        ResponseEntity<Result> response = rest.postForEntity(baseUrl, request, Result.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("20001", response.getBody().getCode());
    }
}