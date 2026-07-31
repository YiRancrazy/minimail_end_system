package com.yirancrazy.minimall.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserCreateDTO description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class UserCreateDTO {

    @NotBlank(message = "username cannot be blank")
    @Size(min = 3, max = 50, message = "username length must be between 3 and 50")
    private String username;

    @NotBlank(message = "nickname cannot be blank")
    @Size(min = 1, max = 100, message = "nickname length must be between 1 and 100")
    private String nickname;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "phone number format is invalid")
    private String phone;

    @Email(message = "email format is invalid")
    private String email;
}