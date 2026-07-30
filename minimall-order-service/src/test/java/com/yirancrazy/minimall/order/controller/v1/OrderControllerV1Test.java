package com.yirancrazy.minimall.order.controller.v1;

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
import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.api.feign.NotifyFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.dto.OrderCreateDTO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderControllerV1 的单元测试类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderControllerV1Test {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @MockBean
    StockFeignClient stockFeign;

    @MockBean
    PayFeignClient payFeign;

    @MockBean
    NotifyFeignClient notifyFeign;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/order";
        when(stockFeign.reserve(any(StockReserveDTO.class))).thenReturn(true);
        when(payFeign.create(any(PayCreateDTO.class))).thenReturn(9999L);
        when(notifyFeign.push(any(NotifyEventDTO.class))).thenReturn(true);
    }

    @Test
    void create_should_success_with_valid_params() {
        OrderCreateDTO dto = new OrderCreateDTO(1L, 100L, 2);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<OrderCreateDTO> request = new HttpEntity<>(dto, headers);

        ResponseEntity<Result> response = rest.postForEntity(baseUrl, request, Result.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("00000", response.getBody().getCode());
    }

    @Test
    void create_should_fail_when_userId_is_null() {
        OrderCreateDTO dto = new OrderCreateDTO(null, 100L, 2);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<OrderCreateDTO> request = new HttpEntity<>(dto, headers);

        ResponseEntity<Result> response = rest.postForEntity(baseUrl, request, Result.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("20001", response.getBody().getCode());
    }

    @Test
    void create_should_fail_when_quantity_is_zero() {
        OrderCreateDTO dto = new OrderCreateDTO(1L, 100L, 0);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<OrderCreateDTO> request = new HttpEntity<>(dto, headers);

        ResponseEntity<Result> response = rest.postForEntity(baseUrl, request, Result.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("20001", response.getBody().getCode());
    }
}