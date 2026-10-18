package com.yirancrazy.minimall.goods.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuListVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpuListVO {
    private Long spuId;
    private String spuNo;
    private String title;
    private String subtitle;
    private String mainImageUrl;
    private Long merchantId;

    private BigDecimal minPrice;
}
