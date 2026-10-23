package com.yirancrazy.minimall.user.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import com.yirancrazy.minimall.api.dto.user.UserProvisionDTO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.service.UserService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalUserControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
public class InternalUserControllerV1Test {

    private UserService userService;
    private InternalUserControllerV1 controller;

    /**
     * 初始化测试依赖。
     */
    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        controller = new InternalUserControllerV1(userService);
    }

    /**
     * 验证内部建档请求委派到服务层。
     */
    @Test
    void provision_calls_service() {
        Result<Void> result = controller.provision(new UserProvisionDTO(9001L));

        assertEquals("00000", result.getCode());
        verify(userService).provision(9001L);
    }
}
