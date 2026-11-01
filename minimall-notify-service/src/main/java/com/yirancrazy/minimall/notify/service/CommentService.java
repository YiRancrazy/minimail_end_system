package com.yirancrazy.minimall.notify.service;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.notify.dto.CommentCreateDto;
import com.yirancrazy.minimall.notify.dto.CommentPageDto;
import com.yirancrazy.minimall.notify.entity.CommentPo;
import com.yirancrazy.minimall.notify.vo.CommentStatsVo;
import com.yirancrazy.minimall.notify.vo.CommentVo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义商品评价的创建、游标查询、商家回复和评分统计能力；创建前必须通过订单服务校验购买资格.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface CommentService {

    /**
     * 为当前用户创建一条商品评价，并以订单服务返回的商家归属覆盖请求体中的商家编号.
     *
     * @param userId 发起评价的用户编号
     * @param dto 评价内容及关联订单商品信息
     * @return 新建评价主键
     * @throws com.yirancrazy.minimall.common.exception.BizException
     *     订单不可评价、远程校验不可用或评价已存在时抛出
     */
    Long create(Long userId, CommentCreateDto dto);

    /**
     * 按游标倒序查询评价持久化记录，并把可选的 SPU、用户、商家、订单号和状态转为过滤条件.
     *
     * @param dto 游标及评价筛选条件
     * @return 当前页评价实体及下一页游标
     */
    CursorPageVo<CommentPo> page(CommentPageDto dto);

    /**
     * 按游标查询评价并映射为对外视图，图片对象键会结合 MinioUtil 转换为可访问地址.
     *
     * @param dto 游标及评价筛选条件
     * @return 当前页评价视图及下一页游标
     */
    CursorPageVo<CommentVo> pageVo(CommentPageDto dto);

    /**
     * 以商家身份回复评价，并要求评价记录中的商家编号与当前商家一致.
     *
     * @param id 评价主键
     * @param merchantId 当前登录商家编号，用于归属校验
     * @param reply 商家回复正文
     * @throws com.yirancrazy.minimall.common.exception.BizException 评价不存在或不属于该商家时抛出
     */
    void reply(Long id, Long merchantId, String reply);

    /**
     * 统计指定 SPU 下正常状态评价的总数、两位小数平均分及各评分数量；无评价时返回全零.
     *
     * @param spuId 商品 SPU 编号
     * @return 评分统计结果
     */
    CommentStatsVo stats(Long spuId);
}
