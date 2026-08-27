package com.yirancrazy.minimall.notify.vo;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.Data;
import com.yirancrazy.minimall.common.util.MinioUtil;
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
     * PO → VO 转换，图片 objectKey 原样返回（供内部或无需解析场景使用）。
     * @param po 评价持久化对象
     * @return 评价视图对象
     */
    public static CommentVO from(CommentPO po) {
        return from(po, null);
    }

    /**
     * PO → VO 转换，额外将 images 中逗号分隔的图片 objectKey 逐个解析为可访问 URL（minioUtil 为 null 时原样返回）。
     * @param po 评价持久化对象
     * @param minioUtil 对象存储工具，用于生成图片预签名 URL
     * @return 评价视图对象
     */
    public static CommentVO from(CommentPO po, MinioUtil minioUtil) {
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
        vo.setImages(resolveImages(po.getImages(), minioUtil));
        vo.setAnonymous(po.getAnonymous());
        vo.setStatus(po.getStatus());
        vo.setMerchantReply(po.getMerchantReply());
        vo.setMerchantReplyTime(po.getMerchantReplyTime());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }

    /**
     * images 为逗号分隔的多个图片 objectKey，统一解析为可访问 URL 后以逗号重新拼接；空值或 blank 原样返回。
     * @param images 逗号分隔的图片 objectKey 串
     * @param minioUtil 对象存储工具
     * @return 解析后的逗号分隔 URL 串
     */
    private static String resolveImages(String images, MinioUtil minioUtil) {
        if (images == null || images.isBlank() || minioUtil == null) {
            return images;
        }
        return Arrays.stream(images.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(minioUtil::resolvePublicUrl)
            .collect(Collectors.joining(","));
    }
}
