package com.yirancrazy.minimall.user.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerV1Test {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    UserService userService;

    @Test
    void create_then_get() {
        UserPO u = new UserPO();
        u.setUsername("bob");
        u.setNickname("Bobby");
        Long id = userService.create(u);

        ResponseEntity<Result> r = rest.getForEntity(
            "http://localhost:" + port + "/api/v1/user/" + id, Result.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("00000", r.getBody().getCode());
    }
}