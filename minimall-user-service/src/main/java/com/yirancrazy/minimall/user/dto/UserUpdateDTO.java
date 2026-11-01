package com.yirancrazy.minimall.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台侧修改用户资料的请求模型，约束昵称、手机号和邮箱的格式与长度.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class UserUpdateDto {

    @NotBlank(message = "nickname cannot be blank")
    @Size(min = 1, max = 100, message = "nickname length must be between 1 and 100")
    private String nickname;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone number format is invalid")
    private String phone;

    @Email(message = "email format is invalid")
    private String email;
}
