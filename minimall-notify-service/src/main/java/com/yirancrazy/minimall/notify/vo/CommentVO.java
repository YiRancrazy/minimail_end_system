package com.yirancrazy.minimall.notify.vo;

import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.notify.entity.CommentPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品评价视图对象，对外暴露时不返回 isDeleted/version 等内部字段
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
public class CommentVO {

    private Long id;
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
    private LocalDateTime merchantReplyTime;
    private LocalDateTime createTime;

    /**
     * PO → VO 转换。
     * @param po 评价持久化对象
     * @return 评价视图对象
     */
    public static CommentVO from(CommentPO po) {
        if (po == null) {
            return null;
        }
        CommentVO vo = new CommentVO();
        vo.setId(po.getId());
        vo.setOrderNo(po.getOrderNo());
        vo.setSpuId(po.getSpuId());
        vo.setSkuId(po.getSkuId());
        vo.setUserId(po.getUserId());
        vo.setMerchantId(po.getMerchantId());
        vo.setRating(po.getRating());
        vo.setContent(po.getContent());
        vo.setImages(po.getImages());
        vo.setAnonymous(po.getAnonymous());
        vo.setStatus(po.getStatus());
        vo.setMerchantReply(po.getMerchantReply());
        vo.setMerchantReplyTime(po.getMerchantReplyTime());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
