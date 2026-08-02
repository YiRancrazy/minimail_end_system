package com.yirancrazy.minimall.goods.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: SPU 列表视图对象，用于用户端商品列表展示
 * @Version: 1.0
 * @DateTime: 2026/08/02
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
}
