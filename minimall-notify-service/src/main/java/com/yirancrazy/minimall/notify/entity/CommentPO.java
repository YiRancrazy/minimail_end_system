package com.yirancrazy.minimall.notify.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CommentPO 持久化对象，对应数据库表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notify_comment")
public class CommentPO extends BasePO {


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
