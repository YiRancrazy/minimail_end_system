package com.yirancrazy.minimall.goods.vo;

import com.yirancrazy.minimall.goods.entity.SpuPo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 输出商家及平台端的 SPU 主数据、状态和嵌套 SKU 视图。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class SpuVo {

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

    private List<SkuVo> skus = new ArrayList<>();

    /**
     * 将 SPU 持久化对象映射为视图对象，集合字段保持默认空列表。
     *
     * @param po 待转换的持久化对象
     * @return 转换后的 SPU 视图对象
     */
    public static SpuVo from(SpuPo po) {
        SpuVo vo = new SpuVo();
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
