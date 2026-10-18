package com.yirancrazy.minimall.notify.vo;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.Data;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.entity.CommentPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CommentVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
     */
    public static CommentVO from(CommentPO po) {
        return from(po, null);
    }


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @param minioUtil minioUtil 参数
     * @return 处理结果
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
     * 转换或解析Images。
     * @param images images
     * @param minioUtil minioUtil
     * @return 处理结果
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
