package com.yirancrazy.minimall.pay.service;

import com.yirancrazy.minimall.api.dto.pay.RefundCreateDto;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.pay.dto.AlipayCallbackDto;
import com.yirancrazy.minimall.pay.dto.PayCallbackDto;
import com.yirancrazy.minimall.pay.dto.PayPageDto;
import com.yirancrazy.minimall.pay.dto.PayStatementDto;
import com.yirancrazy.minimall.pay.entity.PayTransactionPo;
import com.yirancrazy.minimall.pay.vo.PayStatementVo;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVo;
import com.yirancrazy.minimall.pay.vo.PayTransactionVo;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVo;
import com.yirancrazy.minimall.pay.vo.RefundVo;
import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义支付服务的业务边界。
 * 覆盖支付单创建与查询、渠道回调、退款、统计导出及定时补偿，并统一异常和状态语义。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface PayService {

    /**
     * 创建或幂等复用支付单。
     * 订单归属、应付金额和订单状态以订单快照为准；仅支付宝渠道可用，过期待支付流水会关闭后重建。
     *
     * @param orderNo 业务订单号
     * @param userId 支付发起用户
     * @param merchantId 调用方提示的商家 ID，最终以订单快照为准
     * @param amount 支付金额，最多两位小数
     * @param channel 支付渠道 code，为空时默认支付宝
     * @return 新建或复用的支付单主键
     */
    Long createPayment(
            String orderNo, Long userId, Long merchantId, BigDecimal amount, Integer channel);

    /**
     * 处理归一化支付回调并推进本地支付、订单状态。
     * 已有成功单和金额不一致回调具有幂等或冻结保护。
     *
     * @param dto 渠道回调结果
     */
    void handleCallback(PayCallbackDto dto);

    /**
     * 以游标方式查询全平台支付流水实体。
     *
     * @param dto 游标、状态和时间筛选条件
     * @return 支付流水实体游标页
     */
    CursorPageVo<PayTransactionPo> platformPage(PayPageDto dto);

    /**
     * 验签支付宝异步通知并转换为统一支付回调。
     * 业务或验签失败返回 false，使支付宝保留重试语义。
     *
     * @param dto 支付宝原始回调参数
     * @return 回调已成功受理时返回 true
     */
    boolean handleAlipayCallback(AlipayCallbackDto dto);

    /**
     * 按筛选条件查询支付流水实体供导出使用。
     * 结果数量受公共导出上限约束。
     *
     * @param dto 状态和时间筛选条件
     * @return 待导出的支付流水列表
     */
    List<PayTransactionPo> exportTransactions(PayPageDto dto);

    /**
     * 幂等创建或恢复支付退款。
     * 校验支付成功状态与累计退款额度，并处理渠道超时后的待补偿状态。
     *
     * @param dto 支付单、金额、原因和退款请求号
     * @return 已有、恢复或新完成的退款结果
     */
    RefundVo createRefund(RefundCreateDto dto);

    /**
     * 查询当前用户指定订单号的最新支付流水。
     *
     * @param orderNo 业务订单号
     * @param userId 当前用户 ID
     * @return 当前用户拥有的支付流水实体
     */
    PayTransactionPo getByOrderNo(String orderNo, Long userId);

    /**
     * 查询并转换当前用户订单的最新支付状态视图。
     *
     * @param orderNo 业务订单号
     * @param userId 当前用户 ID
     * @return 支付流水视图
     */
    PayTransactionVo getStatusVo(String orderNo, Long userId);

    /**
     * 生成当前用户支付单的支付宝收银台参数。
     *
     * @param paymentNo 支付单号
     * @param userId 当前用户 ID
     * @return 支付主体、金额、过期时间和签名表单
     */
    PaymentParamsVo getPaymentParams(String paymentNo, Long userId);

    /**
     * 关闭指定支付单的渠道交易并同步本地状态。
     * 仅待支付单可关闭，已关闭单幂等返回；渠道关闭成功后释放活跃订单号。
     *
     * @param paymentNo 支付单号
     * @return 渠道关闭结果
     */
    String closePayment(String paymentNo);

    /**
     * 以游标方式查询指定商家的支付流水实体。
     *
     * @param merchantId 商家 ID
     * @param dto 游标、状态和时间筛选条件
     * @return 商家支付流水实体游标页
     */
    CursorPageVo<PayTransactionPo> page(Long merchantId, PayPageDto dto);

    /**
     * 以游标方式查询指定商家的支付流水视图。
     *
     * @param merchantId 商家 ID
     * @param dto 游标、状态和时间筛选条件
     * @return 商家支付流水视图游标页
     */
    CursorPageVo<PayTransactionVo> pageVo(Long merchantId, PayPageDto dto);

    /**
     * 以游标方式查询全平台支付流水视图。
     *
     * @param dto 游标、状态和时间筛选条件
     * @return 平台支付流水视图游标页
     */
    CursorPageVo<PayTransactionVo> platformPageVo(PayPageDto dto);

    /**
     * 统计指定商家或全平台的成功金额、退款金额和交易笔数。
     *
     * @param merchantId 商家 ID，为空时表示全平台
     * @param dto 时间范围筛选条件
     * @return 支付统计视图
     */
    PayStatisticsVo statistics(Long merchantId, PayPageDto dto);

    /**
     * 将指定支付单置为冻结状态，供人工风控处置。
     *
     * @param paymentNo 支付单号
     */
    void freeze(String paymentNo);

    /**
     * 按筛选条件查询并转换支付流水导出视图。
     *
     * @param dto 状态和时间筛选条件
     * @return 支付流水视图列表
     */
    List<PayTransactionVo> exportTransactionVos(PayPageDto dto);

    /**
     * 按日期字符串构造筛选条件并导出支付流水视图。
     * 空日期表示不限边界，非法日期由实现忽略对应边界。
     *
     * @param startDate 起始日期，可为空
     * @param endDate 结束日期，可为空
     * @return 支付流水视图列表
     */
    List<PayTransactionVo> exportTransactionVos(String startDate, String endDate);

    /**
     * 生成指定日期区间的平台支付对账单。
     * 结束日期按整日包含。
     *
     * @param dto 起止日期
     * @return 支付、退款和冻结维度汇总
     */
    PayStatementVo statement(PayStatementDto dto);

    /**
     * 补偿扫描近期已支付流水中订单推进未成功的记录。
     * 每次最多处理十个一百条批次，并以支付时间窗口和 ID 游标限制扫描范围。
     *
     * @return 成功推进订单的支付流水数量
     */
    int scanPaidButOrderPending();

    /**
     * 补偿扫描近期成功但尚未通知订单服务的退款。
     *
     * @return 本次成功通知订单服务的退款数量
     */
    int scanUnnotifiedRefunds();

    /**
     * 抢占并恢复待发起或处理中的退款。
     *
     * @return 本次完成退款的记录数量
     */
    int scanPendingRefunds();
}
