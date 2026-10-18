package com.yirancrazy.minimall.cart.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartMoveToFavoriteDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class CartMoveToFavoriteDTO {


    @NotEmpty(message = "购物车条目不能为空")
    private List<Long> itemIds;
}
