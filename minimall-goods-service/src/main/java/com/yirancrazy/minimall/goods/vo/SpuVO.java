package com.yirancrazy.minimall.goods.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import com.yirancrazy.minimall.goods.entity.SpuPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品SPU VO，用于Controller边界输出，隐藏内部字段
 * @Version: 1.1
 * @DateTime: 2026/08/04
 **/
@Data
public class SpuVO {

    private Long id;
    private String spuNo;
    private Long merchantId;
    private Long categoryId;
    private String title;
    private String subtitle;
    private String mainImageUrl;
    private Integer status;
    private LocalDateTime publishAt;
    private LocalDateTime createTime;
    /** 关联 SKU 列表，由分页查询批量装配，单查场景为空 */
    private List<SkuVO> skus = new ArrayList<>();

    /**
     * 将 SpuPO 转换为 SpuVO。
     * @param po SPU持久化对象
     * @return SPU VO
     */
    public static SpuVO from(SpuPO po) {
        SpuVO vo = new SpuVO();
        vo.setId(po.getId());
        vo.setSpuNo(po.getSpuNo());
        vo.setMerchantId(po.getMerchantId());
        vo.setCategoryId(po.getCategoryId());
        vo.setTitle(po.getTitle());
        vo.setSubtitle(po.getSubtitle());
        vo.setMainImageUrl(po.getMainImageUrl());
        vo.setStatus(po.getStatus());
        vo.setPublishAt(po.getPublishAt());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
