package com.yirancrazy.minimall.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_goods_spu 表，保存商品标题、主图、类目、店铺和审核状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@TableName("t_goods_spu")
public class SpuPo extends BasePo {
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
