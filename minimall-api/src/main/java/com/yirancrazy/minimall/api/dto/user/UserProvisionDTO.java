package com.yirancrazy.minimall.api.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户业务档案建档请求。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProvisionDTO {

    @NotNull(message = "userId cannot be null")
    private Long userId;
}
