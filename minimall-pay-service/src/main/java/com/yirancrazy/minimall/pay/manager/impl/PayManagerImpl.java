package com.yirancrazy.minimall.pay.manager.impl;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.pay.dto.PayStatementResultDTO;
import com.yirancrazy.minimall.pay.dto.PayStatisticsDTO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.manager.PayManager;
import com.yirancrazy.minimall.pay.mapper.PayTransactionMapper;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class PayManagerImpl extends ServiceImpl<PayTransactionMapper, PayTransactionPO>
    implements PayManager {


    /**
     * 执行 statistics 方法。
     * @param merchantId merchantId 参数
     * @param startTime startTime 参数
     * @param endTime endTime 参数
     * @return 处理结果
     */
    @Override
    public PayStatisticsDTO statistics(Long merchantId, LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.statistics(merchantId, startTime, endTime);
    }


    /**
     * 更新业务数据。
     * @param paymentNo paymentNo 参数
     * @param fromStatus fromStatus 参数
     * @param toStatus toStatus 参数
     * @return 业务数值
     */
    @Override
    public int updateStatusIf(String paymentNo, int fromStatus, int toStatus) {
        return baseMapper.updateStatusIf(paymentNo, fromStatus, toStatus);
    }


    /**
     * 执行 statement 方法。
     * @param startTime startTime 参数
     * @param endTime endTime 参数
     * @return 处理结果
     */
    @Override
    public PayStatementResultDTO statement(LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.statement(startTime, endTime);
    }
}
