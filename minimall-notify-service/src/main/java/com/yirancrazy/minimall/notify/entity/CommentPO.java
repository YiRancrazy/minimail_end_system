package com.yirancrazy.minimall.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射 t_notify_comment 商品评价记录，保存评价归属、评分内容、公开状态及商家回复信息.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notify_comment")
public class CommentPo extends BasePo {

  private String orderNo;

  private Long spuId;

  private Long skuId;

  private Long userId;

  private Long merchantId;

  private Integer rating;

  private String content;

  private String images;

  private Integer anonymous;

  private Integer status;

  private String merchantReply;

  private java.time.LocalDateTime merchantReplyTime;
}
