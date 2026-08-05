package com.yirancrazy.minimall.goods.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Spu 分页查询入参，支持按商家、状态、标题过滤
 * @Version: 2.0
 * @DateTime: 2026/08/04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SpuPageDTO extends CursorPageDTO {

    private Long merchantId;
    private Integer status;
    private String title;
}
