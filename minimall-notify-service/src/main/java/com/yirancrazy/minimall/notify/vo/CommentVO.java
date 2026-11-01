package com.yirancrazy.minimall.notify.vo;

import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.entity.CommentPo;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 评价对外视图，提供订单商品归属、评分内容、匿名状态、审核状态、商家回复和图片访问地址.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class CommentVo {

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
     * 将评价实体转换为视图，不解析图片对象键，原样保留 images 字段.
     *
     * @param po 评价实体
     * @return 评价视图；po 为 null 时返回 null
     */
    public static CommentVo from(CommentPo po) {
        return from(po, null);
    }

    /**
     * 将评价实体转换为视图，并在提供 MinioUtil 时把逗号分隔图片对象键解析为公开访问地址.
     *
     * @param po 评价实体
     * @param minioUtil 图片对象键解析工具；为空时保留原始 images
     * @return 评价视图；po 为 null 时返回 null
     */
    public static CommentVo from(CommentPo po, MinioUtil minioUtil) {
        if (po == null) {
            return null;
        }
        CommentVo vo = new CommentVo();
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
     * 拆分逗号分隔的图片对象键，去除空项后逐个解析为公开 URL；原始值为空或工具为空时直接返回原值.
     *
     * @param images 逗号分隔的图片对象键
     * @param minioUtil 图片地址解析工具
     * @return 逗号分隔的公开图片地址或原始值
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
