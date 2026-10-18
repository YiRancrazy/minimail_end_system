package com.yirancrazy.minimall.goods.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import com.yirancrazy.minimall.goods.entity.SpuPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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

    private List<SkuVO> skus = new ArrayList<>();


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
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
