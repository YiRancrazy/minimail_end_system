package com.yirancrazy.minimall.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AdminUpdateDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class AdminUpdateDTO {

    @Size(max = 64, message = "nickname length must be <= 64")
    private String nickname;
}
