package com.yirancrazy.minimall.goods.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台审核驳回入参，必须填写驳回原因
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@Data
public class SpuRejectDTO {

    @NotBlank(message = "reason cannot be blank")
    @Size(max = 512, message = "reason length must be <= 512")
    private String reason;
}
