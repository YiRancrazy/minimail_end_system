package com.yirancrazy.minimall.platform.gateway.impl;

import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVo;
import com.yirancrazy.minimall.api.dto.order.OrderExportItemDto;
import com.yirancrazy.minimall.api.dto.order.OrderStatisticsDto;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDto;
import com.yirancrazy.minimall.api.dto.user.UserManageVo;
import com.yirancrazy.minimall.api.feign.MerchantFeignClient;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.UserFeignClient;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.gateway.PlatformRemoteGateway;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通过用户、商家、订单和支付 Feign 客户端实现平台远程访问边界.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Component
@RequiredArgsConstructor
public class PlatformRemoteGatewayImpl implements PlatformRemoteGateway {

    private final UserFeignClient userFeignClient;
    private final MerchantFeignClient merchantFeignClient;
    private final OrderFeignClient orderFeignClient;
    private final PayFeignClient payFeignClient;

    /**
     * 将用户分页条件转发给用户服务的管理分页接口.
     *
     * @param query 用户管理分页条件
     * @return 用户服务响应，不在网关层改写状态或数据
     */
    @Override
    public Result<CursorPageVo<UserManageVo>> pageUsers(InternalPageQuery query) {
        return userFeignClient.pageManage(query);
    }

    /**
     * 调用用户服务查询用户管理详情.
     *
     * @param userId 用户 ID
     * @return 用户服务响应，不在网关层改写结果
     */
    @Override
    public Result<UserManageVo> userDetail(Long userId) {
        return userFeignClient.detail(userId);
    }

    /**
     * 将商家分页条件转发给商家服务的管理分页接口.
     *
     * @param query 商家管理分页条件
     * @return 商家服务响应，不在网关层改写状态或数据
     */
    @Override
    public Result<CursorPageVo<MerchantManageVo>> pageMerchants(InternalPageQuery query) {
        return merchantFeignClient.pageManage(query);
    }

    /**
     * 调用商家服务查询商家管理详情.
     *
     * @param merchantId 商家 ID
     * @return 商家服务响应，不在网关层改写结果
     */
    @Override
    public Result<MerchantManageVo> merchantDetail(Long merchantId) {
        return merchantFeignClient.detail(merchantId);
    }

    /**
     * 调用商家服务提交资质审核结果.
     *
     * @param merchantId 商家 ID
     * @param approved 是否审核通过
     * @param reason 审核说明或拒绝原因
     * @return 商家服务审核响应
     */
    @Override
    public Result<Void> auditMerchant(Long merchantId, Boolean approved, String reason) {
        return merchantFeignClient.audit(merchantId, approved, reason);
    }

    /**
     * 调用订单服务查询平台订单状态统计.
     *
     * @return 订单服务统计响应
     */
    @Override
    public Result<OrderStatisticsDto> orderStatistics() {
        return orderFeignClient.statistics();
    }

    /**
     * 调用订单服务查询订单导出明细.
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 订单服务导出响应
     */
    @Override
    public Result<List<OrderExportItemDto>> exportOrders(String startDate, String endDate) {
        return orderFeignClient.exportList(startDate, endDate);
    }

    /**
     * 调用支付服务查询支付流水导出明细.
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 支付服务导出响应
     */
    @Override
    public Result<List<PayExportItemDto>> exportPayTransactions(String startDate, String endDate) {
        return payFeignClient.exportTransactions(startDate, endDate);
    }
}
