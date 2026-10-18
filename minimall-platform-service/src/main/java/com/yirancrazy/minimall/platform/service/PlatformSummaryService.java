package com.yirancrazy.minimall.platform.service;

import java.util.List;
import com.yirancrazy.minimall.api.dto.order.OrderExportItemDTO;
import com.yirancrazy.minimall.api.dto.pay.PayExportItemDTO;
import com.yirancrazy.minimall.platform.vo.PlatformOrderSummaryVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformSummaryService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface PlatformSummaryService {


    /**
     * 执行 orderSummary 方法。
     * @return 处理结果
     */
    PlatformOrderSummaryVO orderSummary();


    /**
     * 执行 exportOrders 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @return 结果列表
     */
    List<OrderExportItemDTO> exportOrders(String startDate, String endDate);


    /**
     * 执行 exportReconciliation 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @return 结果列表
     */
    List<PayExportItemDTO> exportReconciliation(String startDate, String endDate);
}
