package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台管理员更新 DTO，仅允许修改昵称
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class AdminUpdateDTO {

    @Size(max = 64, message = "nickname length must be <= 64")
    private String nickname;
}
