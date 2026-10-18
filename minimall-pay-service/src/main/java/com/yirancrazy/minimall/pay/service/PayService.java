package com.yirancrazy.minimall.pay.service;

import java.math.BigDecimal;
import java.util.List;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.pay.dto.AlipayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.dto.PayStatementDTO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.vo.PayStatementVO;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;
import com.yirancrazy.minimall.pay.vo.PayTransactionVO;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVO;
import com.yirancrazy.minimall.pay.vo.RefundVO;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface PayService {

    /**
     * 创建业务数据。
     * @param orderNo orderNo 参数
     * @param userId userId 参数
     * @param merchantId merchantId 参数
     * @param amount amount 参数
     * @param channel channel 参数
     * @return 业务数值
     */
    Long createPayment(String orderNo, Long userId, Long merchantId, BigDecimal amount, Integer channel);


    /**
     * 处理业务事件。
     * @param dto dto 参数
     */
    void handleCallback(PayCallbackDTO dto);


    /**
     * 执行 platformPage 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<PayTransactionPO> platformPage(PayPageDTO dto);
    /**
     * 处理业务事件。
     * @param dto dto 参数
     * @return 是否处理成功
     */
    boolean handleAlipayCallback(AlipayCallbackDTO dto);


    /**
     * 执行 exportTransactions 方法。
     * @param dto dto 参数
     * @return 结果列表
     */
    List<PayTransactionPO> exportTransactions(PayPageDTO dto);
    /**
     * 创建业务数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    RefundVO createRefund(RefundCreateDTO dto);



    /**
     * 获取相关数据。
     * @param orderNo orderNo 参数
     * @param userId userId 参数
     * @return 处理结果
     */
    PayTransactionPO getByOrderNo(String orderNo, Long userId);
    /**
     * 获取相关数据。
     * @param orderNo orderNo 参数
     * @param userId userId 参数
     * @return 处理结果
     */
    PayTransactionVO getStatusVO(String orderNo, Long userId);


    /**
     * 获取相关数据。
     * @param paymentNo paymentNo 参数
     * @param userId userId 参数
     * @return 处理结果
     */
    PaymentParamsVO getPaymentParams(String paymentNo, Long userId);



    /**
     * 分页查询数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<PayTransactionPO> page(Long merchantId, PayPageDTO dto);
    /**
     * 分页查询数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<PayTransactionVO> pageVO(Long merchantId, PayPageDTO dto);



    /**
     * 执行 platformPageVO 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<PayTransactionVO> platformPageVO(PayPageDTO dto);


    /**
     * 执行 statistics 方法。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 处理结果
     */
    PayStatisticsVO statistics(Long merchantId, PayPageDTO dto);


    /**
     * 执行 freeze 方法。
     * @param paymentNo paymentNo 参数
     */
    void freeze(String paymentNo);



    /**
     * 执行 exportTransactionVOs 方法。
     * @param dto dto 参数
     * @return 结果列表
     */
    List<PayTransactionVO> exportTransactionVOs(PayPageDTO dto);


    /**
     * 执行 exportTransactionVOs 方法。
     * @param startDate startDate 参数
     * @param endDate endDate 参数
     * @return 结果列表
     */
    List<PayTransactionVO> exportTransactionVOs(String startDate, String endDate);


    /**
     * 执行 statement 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    PayStatementVO statement(PayStatementDTO dto);


    /**
     * 扫描并处理待办数据。
     * @return 业务数值
     */
    int scanPaidButOrderPending();


    /**
     * 扫描并处理待办数据。
     * @return 业务数值
     */
    int scanUnnotifiedRefunds();


    /**
     * 扫描并处理待办数据。
     * @return 业务数值
     */
    int scanPendingRefunds();
}
