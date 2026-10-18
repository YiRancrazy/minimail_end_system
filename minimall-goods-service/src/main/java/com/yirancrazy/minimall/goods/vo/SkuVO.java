package com.yirancrazy.minimall.goods.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.goods.entity.SkuPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SkuVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class SkuVO {

    private Long id;
    private Long spuId;
    private String skuName;
    private BigDecimal price;
    private Integer stock;
    private LocalDateTime createTime;


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
     */
    public static SkuVO from(SkuPO po) {
        SkuVO vo = new SkuVO();
        vo.setId(po.getId());
        vo.setSpuId(po.getSpuId());
        vo.setSkuName(po.getSkuName());
        vo.setPrice(po.getPrice());
        vo.setStock(po.getStock());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
