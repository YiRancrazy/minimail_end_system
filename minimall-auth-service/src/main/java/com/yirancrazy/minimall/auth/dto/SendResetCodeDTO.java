package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 发送重置密码验证码数据传输对象，按用户名生成验证码。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendResetCodeDTO {

    @NotBlank(message = "username cannot be blank")
    private String username;
}
