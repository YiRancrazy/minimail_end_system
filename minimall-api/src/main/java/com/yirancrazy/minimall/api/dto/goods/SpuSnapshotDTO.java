package com.yirancrazy.minimall.api.dto.goods;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuSnapshotDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpuSnapshotDTO {
    private Long spuId;
    private String title;
    private String mainImageUrl;
}
