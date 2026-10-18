package com.yirancrazy.minimall.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: RefreshTokenDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenDTO {
    @NotBlank
    private String refreshToken;
}