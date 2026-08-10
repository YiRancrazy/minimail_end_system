package com.yirancrazy.minimall.notify.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品评价分页查询入参，支持按 SPU/用户/商家/订单号过滤
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class CommentPageDTO extends CursorPageDTO {

    /** 按 SPU ID 过滤，null 不限制 */
    private Long spuId;

    /** 按用户 ID 过滤，null 不限制 */
    private Long userId;

    /** 按商家 ID 过滤，null 不限制 */
    private Long merchantId;

    /** 按订单号精确匹配，null 不限制 */
    private String orderNo;

    /** 仅返回正常状态，null 表示不过滤 */
    private Integer status;
}
