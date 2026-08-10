package com.yirancrazy.minimall.notify.service;

import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.entity.CommentPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台评价管理领域服务接口，定义评价审核（通过/隐藏）、平台回复能力
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
public interface PlatformCommentService {

    /**
     * 平台分页查询评价，支持按状态/订单号过滤。
     * @param dto 分页入参
     * @return 评价游标分页结果
     */
    CursorPageVO<CommentPO> page(CommentPageDTO dto);

    /**
     * 平台通过评价，将状态置为 NORMAL。
     * @param id 评价ID
     */
    void approve(Long id);

    /**
     * 平台隐藏评价，将状态置为 HIDDEN。
     * @param id 评价ID
     */
    void hide(Long id);

    /**
     * 平台回复评价，写入 merchantReply 字段与回复时间。
     * @param id 评价ID
     * @param reply 回复内容
     */
    void reply(Long id, String reply);
}
