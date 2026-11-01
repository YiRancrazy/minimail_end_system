package com.yirancrazy.minimall.notify.service;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.notify.dto.CommentPageDto;
import com.yirancrazy.minimall.notify.entity.CommentPo;
import com.yirancrazy.minimall.notify.vo.CommentVo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义平台侧评价审核、隐藏和回复能力，并支持按状态与订单号进行游标查询.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface PlatformCommentService {

    /**
     * 按游标查询平台评价实体，支持状态和订单号过滤.
     *
     * @param dto 游标及平台评价筛选条件
     * @return 当前页评价实体及下一页游标
     */
    CursorPageVo<CommentPo> page(CommentPageDto dto);

    /**
     * 按游标查询平台评价并映射为带图片访问地址的视图.
     *
     * @param dto 游标及平台评价筛选条件
     * @return 当前页评价视图及下一页游标
     */
    CursorPageVo<CommentVo> pageVo(CommentPageDto dto);

    /**
     * 将评价置为正常状态；已经是正常状态时不重复更新.
     *
     * @param id 评价主键
     * @throws com.yirancrazy.minimall.common.exception.BizException 评价不存在时抛出
     */
    void approve(Long id);

    /**
     * 将评价置为隐藏状态.
     *
     * @param id 评价主键
     * @throws com.yirancrazy.minimall.common.exception.BizException 评价不存在时抛出
     */
    void hide(Long id);

    /**
     * 以平台身份写入评价回复并记录回复时间.
     *
     * @param id 评价主键
     * @param reply 回复正文
     * @throws com.yirancrazy.minimall.common.exception.BizException 评价不存在时抛出
     */
    void reply(Long id, String reply);
}
