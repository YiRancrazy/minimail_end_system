package com.yirancrazy.minimall.goods.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Spu持久化对象，映射t_goods_spu表，记录商品SPU核心信息。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
@Data
@TableName("t_goods_spu")
public class SpuPO extends BasePO {
    private String spuNo;
    // 归属店铺ID，商品必须挂靠在营业中店铺下，杜绝无店铺发布
    private Long shopId;
    private Long merchantId;
    private Long categoryId;
    private String title;
    private String subtitle;
    private String mainImageUrl;
    private Integer status;
    private LocalDateTime publishAt;
}
