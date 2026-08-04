package com.yirancrazy.minimall.pay.controller.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.pay.dto.PayCallbackDTO;
import com.yirancrazy.minimall.pay.dto.PayCreateDTO;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.dto.PayStatementDTO;
import com.yirancrazy.minimall.pay.dto.WithdrawApplyDTO;
import com.yirancrazy.minimall.pay.entity.MerchantWithdrawPO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayStatementVO;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;
import com.yirancrazy.minimall.pay.vo.PayTransactionVO;
import com.yirancrazy.minimall.pay.vo.PaymentParamsVO;
import com.yirancrazy.minimall.pay.vo.RefundVO;
import com.yirancrazy.minimall.pay.vo.WithdrawVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayControllerV1 类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Slf4j
@RestController
@RequestMapping("/api/v1/pay")
public class PayControllerV1 {

    private final PayService payService;

    public PayControllerV1(PayService payService) {
        this.payService = payService;
    }

    /**
     * 创建支付流水，channel 为空时默认 ALIPAY。
     * @param dto 支付流水创建DTO
     * @return 支付流水ID
     */
    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody PayCreateDTO dto) {
        Long paymentId = payService.createPayment(
            dto.getOrderNo(), dto.getUserId(), dto.getMerchantId(), dto.getAmount(), dto.getChannel());
        return Result.success(paymentId);
    }

    /**
     * 处理支付宝回调。
     * @param request HTTP请求
     * @return 回调结果
     */
    @PostMapping("/callback/alipay")
    public String callback(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> params.put(key, values[0]));

        try {
            String tradeNo = params.get("trade_no");
            String paymentNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            boolean success = "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);

            PayCallbackDTO dto = new PayCallbackDTO(paymentNo, tradeNo, success, params.toString());
            payService.handleCallback(dto);
            return "success";
        }
        catch (BizException e) {
            log.error("callback failed: {}", e.getMessage());
            return "fail";
        }
    }

    /**
     * 创建退款。
     * @param dto 退款创建DTO
     * @return 退款VO
     */
    @PostMapping("/refunds")
    public Result<RefundVO> createRefund(@Valid @RequestBody RefundCreateDTO dto) {
        RefundVO vo = payService.createRefund(dto);
        return Result.success(vo);
    }

    /**
     * 按订单号查询支付流水状态。
     * @param orderNo 订单号
     * @return 支付流水VO
     */
    @GetMapping("/status/{orderNo}")
    public Result<PayTransactionVO> getStatus(@PathVariable("orderNo") String orderNo) {
        return Result.success(PayTransactionVO.from(payService.getByOrderNo(orderNo)));
    }

    /**
     * 按支付单号查询支付参数，供前端调起渠道 SDK。
     * @param paymentNo 支付单号
     * @return 支付参数VO
     */
    @GetMapping("/params/{paymentNo}")
    public Result<PaymentParamsVO> getParams(@PathVariable("paymentNo") String paymentNo) {
        return Result.success(payService.getPaymentParams(paymentNo));
    }

    /**
     * 商家资金流水分页查询，merchantId 由可信 Header 注入。
     * @param merchantId 商家ID
     * @param dto 分页查询入参
     * @return 支付流水分页结果
     */
    @GetMapping("/merchant/transactions")
    public Result<IPage<PayTransactionVO>> merchantTransactions(@RequestHeader("X-Merchant-Id") Long merchantId,
                                                               @Valid PayPageDTO dto) {
        return Result.success(payService.page(merchantId, dto).convert(PayTransactionVO::from));
    }

    /**
     * 平台全平台交易流水分页查询。
     * @param dto 分页查询入参
     * @return 支付流水分页结果
     */
    @GetMapping("/transactions")
    public Result<IPage<PayTransactionVO>> platformTransactions(@Valid PayPageDTO dto) {
        return Result.success(payService.platformPage(dto).convert(PayTransactionVO::from));
    }

    /**
     * 平台资金统计报表，含交易总额、退款总额、交易笔数。
     * @param dto 分页查询入参（复用时间范围字段）
     * @return 统计VO
     */
    @GetMapping("/statistics")
    public Result<PayStatisticsVO> platformStatistics(@Valid PayPageDTO dto) {
        return Result.success(payService.statistics(null, dto));
    }

    /**
     * 平台对账单聚合查询，按日期范围统计交易笔数与金额。
     * @param dto 对账单查询入参（含起止日期）
     * @return 对账单 VO
     */
    @GetMapping("/statements")
    public Result<PayStatementVO> statement(@Valid PayStatementDTO dto) {
        return Result.success(payService.statement(dto));
    }

    /**
     * 商家交易汇总统计，merchantId 由可信 Header 注入。
     * @param merchantId 商家ID
     * @param dto 分页查询入参（复用时间范围字段）
     * @return 统计VO
     */
    @GetMapping("/merchant/statistics")
    public Result<PayStatisticsVO> merchantStatistics(@RequestHeader("X-Merchant-Id") Long merchantId,
                                                     @Valid PayPageDTO dto) {
        return Result.success(payService.statistics(merchantId, dto));
    }

    /**
     * 平台异常支付冻结，将支付单状态置为 FROZEN。
     * @param paymentNo 支付单号
     */
    @PostMapping("/{paymentNo}/freeze")
    public Result<Void> freeze(@PathVariable("paymentNo") String paymentNo) {
        payService.freeze(paymentNo);
        return Result.success(null);
    }

    /**
     * 商家提现申请，merchantId 由可信 Header 注入。
     * @param merchantId 商家ID
     * @param dto 提现申请DTO
     * @return 提现单VO
     */
    @PostMapping("/merchant/withdraw")
    public Result<WithdrawVO> applyWithdraw(@RequestHeader("X-Merchant-Id") Long merchantId,
                                            @Valid @RequestBody WithdrawApplyDTO dto) {
        return Result.success(payService.applyWithdraw(merchantId, dto));
    }

    /**
     * 商家提现记录分页查询，merchantId 由可信 Header 注入。
     * @param merchantId 商家ID
     * @param dto 分页查询入参
     * @return 提现单分页结果
     */
    @GetMapping("/merchant/withdrawals")
    public Result<IPage<MerchantWithdrawPO>> merchantWithdrawals(@RequestHeader("X-Merchant-Id") Long merchantId,
                                                                 @Valid PayPageDTO dto) {
        return Result.success(payService.pageWithdraw(merchantId, dto));
    }

    /**
     * 平台提现记录分页查询。
     * @param dto 分页查询入参
     * @return 提现单分页结果
     */
    @GetMapping("/withdrawals")
    public Result<IPage<MerchantWithdrawPO>> platformWithdrawals(@Valid PayPageDTO dto) {
        return Result.success(payService.platformPageWithdraw(dto));
    }

    /**
     * 平台审核提现申请。
     * @param withdrawId 提现单ID
     * @param approved 是否通过
     * @param reason 驳回原因，approved=false 时填写
     * @return 操作结果
     */
    @PostMapping("/withdrawals/{withdrawId}/review")
    public Result<Void> reviewWithdraw(@PathVariable("withdrawId") Long withdrawId,
                                       @RequestParam boolean approved,
                                       @RequestParam(required = false) String reason) {
        payService.reviewWithdraw(withdrawId, approved, reason);
        return Result.success(null);
    }

    private static final String[] TX_HEADERS = {
        "支付单号", "订单号", "用户ID", "商家ID", "金额", "状态", "创建时间"
    };

    /**
     * 平台导出交易流水对账单 CSV。
     * @param dto 查询入参
     * @param response HTTP 响应
     * @throws IOException 写入失败时抛出
     */
    @GetMapping("/transactions/export")
    public void exportTransactions(@Valid PayPageDTO dto,
                                   HttpServletResponse response) throws IOException {
        List<PayTransactionPO> list = payService.exportTransactions(dto);
        List<String[]> rows = new ArrayList<>(list.size());
        for (PayTransactionPO po : list) {
            rows.add(new String[] {
                po.getPaymentNo(),
                po.getOrderNo(),
                String.valueOf(po.getUserId()),
                String.valueOf(po.getMerchantId()),
                po.getAmount() == null ? "" : po.getAmount().toPlainString(),
                String.valueOf(po.getStatus()),
                po.getCreateTime() == null ? "" : po.getCreateTime().toString()
            });
        }
        CsvExporter.write(response, "transactions.csv", TX_HEADERS, rows);
    }
}