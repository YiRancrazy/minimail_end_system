package com.yirancrazy.minimall.platform.gateway;

import java.util.List;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO;
import com.yirancrazy.minimall.api.dto.order.OrderExportItemDTO;
import com.yirancrazy.minimall.api.dto.order.OrderStatisticsDTO;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDTO;
import com.yirancrazy.minimall.api.dto.user.UserManageVO;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformRemoteGateway 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface PlatformRemoteGateway {


    /**
     * 分页查询数据。
     * @param query query 参数
     * @return 统一响应结果
     */
    Result<CursorPageVO<UserManageVO>> pageUsers(InternalPageQuery query);


    /**
     * 执行 userDetail 方法。
     * @param userId userId 参数
     * @return 统一响应结果
     */
    Result<UserManageVO> userDetail(Long userId);


    /**
     * 分页查询数据。
     * @param query query 参数
     * @return 统一响应结果
     */
    Result<CursorPageVO<MerchantManageVO>> pageMerchants(InternalPageQuery query);


    /**
     * 执行 merchantDetail 方法。
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    Result<MerchantManageVO> merchantDetail(Long merchantId);


    /**
     * 执行 auditMerchant 方法。
     * @param merchantId merchantId 参数
     * @param approved approved 参数
     * @param reason reason 参数
     * @return 统一响应结果
     */
    Result<Void> auditMerchant(Long merchantId, Boolean approved, String reason);


    /**
     * 执行 orderStatistics 方法。
     * @return 统一响应结果
     */
    Result<OrderStatisticsDTO> orderStatistics();


    /**
     * 执行 exportOrders 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @return 统一响应结果
     */
    Result<List<OrderExportItemDTO>> exportOrders(String startDate, String endDate);


    /**
     * 执行 exportPayTransactions 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @return 统一响应结果
     */
    Result<List<PayExportItemDTO>> exportPayTransactions(String startDate, String endDate);
}
