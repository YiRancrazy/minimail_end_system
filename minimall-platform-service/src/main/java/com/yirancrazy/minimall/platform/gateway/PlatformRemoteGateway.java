package com.yirancrazy.minimall.platform.gateway;

import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVo;
import com.yirancrazy.minimall.api.dto.order.OrderExportItemDto;
import com.yirancrazy.minimall.api.dto.order.OrderStatisticsDto;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDto;
import com.yirancrazy.minimall.api.dto.user.UserManageVo;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台服务访问用户、商家、订单和支付服务的远程能力边界.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface PlatformRemoteGateway {

    /**
     * 查询用户服务的用户管理游标分页.
     *
     * @param query 游标、数量、关键词及状态筛选条件
     * @return 原样转发自用户服务的分页响应
     */
    Result<CursorPageVo<UserManageVo>> pageUsers(InternalPageQuery query);

    /**
     * 查询用户服务中的用户管理详情.
     *
     * @param userId 用户 ID
     * @return 原样转发自用户服务的详情响应
     */
    Result<UserManageVo> userDetail(Long userId);

    /**
     * 查询商家服务的商家管理游标分页.
     *
     * @param query 游标、数量、关键词及审核状态筛选条件
     * @return 原样转发自商家服务的分页响应
     */
    Result<CursorPageVo<MerchantManageVo>> pageMerchants(InternalPageQuery query);

    /**
     * 查询商家服务中的商家管理详情.
     *
     * @param merchantId 商家 ID
     * @return 原样转发自商家服务的详情响应
     */
    Result<MerchantManageVo> merchantDetail(Long merchantId);

    /**
     * 将商家资质审核结果提交给商家服务.
     *
     * @param merchantId 商家 ID
     * @param approved 是否审核通过
     * @param reason 审核说明或拒绝原因
     * @return 原样转发自商家服务的审核响应
     */
    Result<Void> auditMerchant(Long merchantId, Boolean approved, String reason);

    /**
     * 查询订单服务的平台订单状态统计.
     *
     * @return 原样转发自订单服务的统计响应
     */
    Result<OrderStatisticsDto> orderStatistics();

    /**
     * 按日期范围查询订单服务的订单导出明细.
     *
     * @param startDate 开始日期，格式由下游服务约定
     * @param endDate 结束日期，格式由下游服务约定
     * @return 原样转发自订单服务的导出响应
     */
    Result<List<OrderExportItemDto>> exportOrders(String startDate, String endDate);

    /**
     * 按日期范围查询支付服务的支付流水导出明细.
     *
     * @param startDate 开始日期，格式由下游服务约定
     * @param endDate 结束日期，格式由下游服务约定
     * @return 原样转发自支付服务的导出响应
     */
    Result<List<PayExportItemDto>> exportPayTransactions(String startDate, String endDate);
}
