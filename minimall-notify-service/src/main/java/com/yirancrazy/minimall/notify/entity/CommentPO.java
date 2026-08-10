package com.yirancrazy.minimall.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品评价持久化对象，映射 t_notify_comment 表
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notify_comment")
public class CommentPO extends BasePO {

    /** 关联订单号 */
    private String orderNo;

    /** 商品 SPU ID */
    private Long spuId;

    /** 商品 SKU ID，可空 */
    private Long skuId;

    /** 评价用户ID */
    private Long userId;

    /** 商家ID，便于按商家维度查询 */
    private Long merchantId;

    /** 评分 1-5 */
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 评价图片，逗号分隔 objectKey 列表 */
    private String images;

    /** 是否匿名 0=否 1=是 */
    private Integer anonymous;

    /** 状态，见 CommentStatusEnum */
    private Integer status;

    /** 商家回复内容 */
    private String merchantReply;

    /** 商家回复时间 */
    private java.time.LocalDateTime merchantReplyTime;
}
