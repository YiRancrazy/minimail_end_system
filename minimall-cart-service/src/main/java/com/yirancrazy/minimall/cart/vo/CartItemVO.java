package com.yirancrazy.minimall.cart.vo;

import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.cart.entity.CartItemPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车项VO，用于Controller边界输出，隐藏内部字段
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Data
public class CartItemVO {

    private Long id;
    private Long userId;
    private Long skuId;
    private Integer quantity;
    private Integer selected;
    private LocalDateTime createTime;

    /**
     * 将 CartItemPO 转换为 CartItemVO。
     * @param po 购物车项持久化对象
     * @return 购物车项VO
     */
    public static CartItemVO from(CartItemPO po) {
        CartItemVO vo = new CartItemVO();
        vo.setId(po.getId());
        vo.setUserId(po.getUserId());
        vo.setSkuId(po.getSkuId());
        vo.setQuantity(po.getQuantity());
        vo.setSelected(po.getSelected());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
