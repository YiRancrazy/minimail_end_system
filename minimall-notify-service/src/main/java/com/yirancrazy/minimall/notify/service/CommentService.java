package com.yirancrazy.minimall.notify.service;

import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.notify.dto.CommentCreateDTO;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.entity.CommentPO;
import com.yirancrazy.minimall.notify.vo.CommentStatsVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品评价领域服务接口，定义评价提交、分页查询、商家回复、统计等业务契约
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
public interface CommentService {

    /**
     * 用户提交商品评价，初始状态为 NORMAL。
     * @param userId 用户ID，来自 X-User-Id Header
     * @param dto 创建入参
     * @return 新评价ID
     */
    Long create(Long userId, CommentCreateDTO dto);

    /**
     * 分页查询评价，支持按 SPU/用户/商家/订单号过滤。
     * @param dto 分页入参
     * @return 评价分页结果
     */
    CursorPageVO<CommentPO> page(CommentPageDTO dto);

    /**
     * 商家回复评价，限同商家（merchantId 匹配）的商家用户调用；已回复时抛错。
     * @param id 评价ID
     * @param merchantId 商家ID，来自 X-Merchant-Id Header
     * @param reply 回复内容
     */
    void reply(Long id, Long merchantId, String reply);

    /**
     * 平台/商家按 SPU 查询评价统计（总数 + 平均分 + 各评分数量）。
     * @param spuId 商品SPU ID
     * @return 评价统计视图
     */
    CommentStatsVO stats(Long spuId);
}
