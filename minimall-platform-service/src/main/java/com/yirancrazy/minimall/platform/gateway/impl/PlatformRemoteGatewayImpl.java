package com.yirancrazy.minimall.platform.gateway.impl;

import java.util.List;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO;
import com.yirancrazy.minimall.api.dto.order.OrderExportItemDTO;
import com.yirancrazy.minimall.api.dto.order.OrderStatisticsDTO;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDTO;
import com.yirancrazy.minimall.api.dto.user.UserManageVO;
import com.yirancrazy.minimall.api.feign.MerchantFeignClient;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.UserFeignClient;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.gateway.PlatformRemoteGateway;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformRemoteGatewayImpl 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Component
@RequiredArgsConstructor
public class PlatformRemoteGatewayImpl implements PlatformRemoteGateway {

    private final UserFeignClient userFeignClient;
    private final MerchantFeignClient merchantFeignClient;
    private final OrderFeignClient orderFeignClient;
    private final PayFeignClient payFeignClient;

    /**
     * 分页查询数据。
     * @param query query 参数
     * @return 统一响应结果
     */
    @Override
    public Result<CursorPageVO<UserManageVO>> pageUsers(InternalPageQuery query) {
        return userFeignClient.pageManage(query);
    }

    /**
     * 执行 userDetail 方法。
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @Override
    public Result<UserManageVO> userDetail(Long userId) {
        return userFeignClient.detail(userId);
    }

    /**
     * 分页查询数据。
     * @param query query 参数
     * @return 统一响应结果
     */
    @Override
    public Result<CursorPageVO<MerchantManageVO>> pageMerchants(InternalPageQuery query) {
        return merchantFeignClient.pageManage(query);
    }

    /**
     * 执行 merchantDetail 方法。
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @Override
    public Result<MerchantManageVO> merchantDetail(Long merchantId) {
        return merchantFeignClient.detail(merchantId);
    }

    /**
     * 执行 auditMerchant 方法。
     * @param merchantId merchantId 参数
     * @param approved approved 参数
     * @param reason reason 参数
     * @return 统一响应结果
     */
    @Override
    public Result<Void> auditMerchant(Long merchantId, Boolean approved, String reason) {
        return merchantFeignClient.audit(merchantId, approved, reason);
    }

    /**
     * 执行 orderStatistics 方法。
     * @return 统一响应结果
     */
    @Override
    public Result<OrderStatisticsDTO> orderStatistics() {
        return orderFeignClient.statistics();
    }

    /**
     * 执行 exportOrders 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @return 统一响应结果
     */
    @Override
    public Result<List<OrderExportItemDTO>> exportOrders(String startDate, String endDate) {
        return orderFeignClient.exportList(startDate, endDate);
    }

    /**
     * 执行 exportPayTransactions 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @return 统一响应结果
     */
    @Override
    public Result<List<PayExportItemDTO>> exportPayTransactions(String startDate, String endDate) {
        return payFeignClient.exportTransactions(startDate, endDate);
    }
}
