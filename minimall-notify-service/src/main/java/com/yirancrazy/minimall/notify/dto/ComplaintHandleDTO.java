package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ComplaintHandleDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class ComplaintHandleDTO {


    @NotNull(message = "status cannot be null")
    private Integer status;


    @NotBlank(message = "result cannot be blank")
    private String result;
}
