package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 投诉处理 DTO，用于平台处理投诉入参校验
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class ComplaintHandleDTO {

    /** 目标状态，必须为 PROCESSING/RESOLVED/REJECTED */
    @NotNull(message = "status cannot be null")
    private Integer status;

    /** 处理结果 */
    @NotBlank(message = "result cannot be blank")
    private String result;
}
