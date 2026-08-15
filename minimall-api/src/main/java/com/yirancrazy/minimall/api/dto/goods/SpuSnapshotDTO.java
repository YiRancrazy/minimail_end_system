package com.yirancrazy.minimall.api.dto.goods;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Spu快照数据传输对象，供购物车等跨服务链路获取商品标题与主图等展示字段。
 * @Version: 1.0
 * @DateTime: 2026/08/13
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpuSnapshotDTO {
    private Long spuId;
    private String title;
    private String mainImageUrl;
}
