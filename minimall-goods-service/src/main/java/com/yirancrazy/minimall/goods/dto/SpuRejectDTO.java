package com.yirancrazy.minimall.goods.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuRejectDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class SpuRejectDTO {

    @NotBlank(message = "reason cannot be blank")
    @Size(max = 512, message = "reason length must be <= 512")
    private String reason;
}
