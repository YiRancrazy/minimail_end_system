package com.yirancrazy.minimall.pay.manager;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.pay.dto.PayStatementResultDTO;
import com.yirancrazy.minimall.pay.dto.PayStatisticsDTO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayManager 管理器接口，封装数据访问能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface PayManager extends IService<PayTransactionPO> {


    /**
     * 执行 statistics 方法。
     * @param merchantId merchantId 参数
     * @param startTime startTime 参数
     * @param endTime endTime 参数
     * @return 处理结果
     */
    PayStatisticsDTO statistics(Long merchantId, LocalDateTime startTime, LocalDateTime endTime);


    /**
     * 更新业务数据。
     * @param paymentNo paymentNo 参数
     * @param fromStatus fromStatus 参数
     * @param toStatus toStatus 参数
     * @return 业务数值
     */
    int updateStatusIf(String paymentNo, int fromStatus, int toStatus);


    /**
     * 执行 statement 方法。
     * @param startTime startTime 参数
     * @param endTime endTime 参数
     * @return 处理结果
     */
    PayStatementResultDTO statement(LocalDateTime startTime, LocalDateTime endTime);
}
