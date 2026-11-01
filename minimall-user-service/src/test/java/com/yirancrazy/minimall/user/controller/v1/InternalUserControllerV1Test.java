package com.yirancrazy.minimall.user.controller.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.yirancrazy.minimall.api.dto.user.UserProvisionDto;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 内部用户控制器单元测试，使用 Mockito 替身验证账号建档请求到 UserService 的委托关系.
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
public class InternalUserControllerV1Test {

    private UserService userService;
    private InternalUserControllerV1 controller;

    /**
     * 初始化用户服务替身和待测控制器。
     */
    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        controller = new InternalUserControllerV1(userService);
    }

    /** 验证内部建档：前置 UserService 替身，提交 userId=9001，预期 00000 且调用 provision. */
    @Test
    void testProvisionCallsService() {
        Result<Void> result = controller.provision(new UserProvisionDto(9001L));

        assertEquals("00000", result.getCode());
        verify(userService).provision(9001L);
    }
}
