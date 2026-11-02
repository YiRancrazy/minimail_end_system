package com.yirancrazy.minimall.notify.service;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.notify.dto.ComplaintCreateDto;
import com.yirancrazy.minimall.notify.dto.ComplaintHandleDto;
import com.yirancrazy.minimall.notify.dto.ComplaintPageDto;
import com.yirancrazy.minimall.notify.entity.ComplaintPo;
import com.yirancrazy.minimall.notify.vo.ComplaintVo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义投诉创建、游标查询、平台处理及按用户查看详情的能力，并强制待处理到处理中的受限状态流转.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface ComplaintService {

    /**
     * 创建待处理投诉，投诉人编号由可信请求头传入并覆盖请求体中的同名字段.
     *
     * @param complainantId 当前登录用户编号
     * @param dto 投诉双方、关联订单及投诉内容
     * @param idempotencyKey 格式化后的投诉幂等键
     * @return 新建投诉主键
     */
    Long create(Long complainantId, ComplaintCreateDto dto, String idempotencyKey);

    /**
     * 按游标倒序查询投诉实体，并支持状态、订单号、投诉人和被投诉人过滤.
     *
     * @param dto 游标及投诉筛选条件
     * @return 当前页投诉实体及下一页游标
     */
    CursorPageVo<ComplaintPo> page(ComplaintPageDto dto);

    /**
     * 按游标查询投诉并映射为对外视图.
     *
     * @param dto 游标及投诉筛选条件
     * @return 当前页投诉视图及下一页游标
     */
    CursorPageVo<ComplaintVo> pageVo(ComplaintPageDto dto);

    /**
     * 按合法状态机处理投诉，并通过带原状态条件的更新防止并发请求重复推进.
     *
     * @param id 投诉主键
     * @param handlerId 平台处理人编号
     * @param dto 目标状态与处理结果
     * @throws com.yirancrazy.minimall.common.exception.BizException 投诉不存在、状态非法或条件更新失败时抛出
     */
    void handle(Long id, Long handlerId, ComplaintHandleDto dto);

    /**
     * 查询投诉实体，不存在时抛出业务异常.
     *
     * @param id 投诉主键
     * @return 投诉实体
     * @throws com.yirancrazy.minimall.common.exception.BizException 投诉不存在时抛出
     */
    ComplaintPo detail(Long id);

    /**
     * 查询投诉并映射为平台侧视图.
     *
     * @param id 投诉主键
     * @return 投诉视图
     * @throws com.yirancrazy.minimall.common.exception.BizException 投诉不存在时抛出
     */
    ComplaintVo detailVo(Long id);

    /**
     * 查询当前用户作为投诉人拥有的投诉；非本人记录按不存在处理，避免泄露他人投诉.
     *
     * @param id 投诉主键
     * @param userId 当前登录用户编号
     * @return 用户侧投诉视图
     * @throws com.yirancrazy.minimall.common.exception.BizException 投诉不存在或不属于当前用户时抛出
     */
    ComplaintVo detailForUser(Long id, Long userId);
}
