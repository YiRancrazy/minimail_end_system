package com.yirancrazy.minimall.pay.controller.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CsvExporter;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.dto.PayStatementDTO;
import com.yirancrazy.minimall.pay.entity.MerchantWithdrawPO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayStatementVO;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;
import com.yirancrazy.minimall.pay.vo.PayTransactionVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端支付控制器，提供平台交易流水分页、资金统计、对账单、异常冻结、提现审核与流水导出。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@RestController
@RequestMapping("/api/v1/platform/pay")
public class PlatformPayControllerV1 {

    private static final String[] TX_HEADERS = {
        "支付单号", "订单号", "用户ID", "商家ID", "金额", "状态", "创建时间"
    };

    private final PayService payService;

    public PlatformPayControllerV1(PayService payService) {
        this.payService = payService;
    }

    /**
     * 平台全平台交易流水游标分页查询。
     * @param dto 游标分页查询入参
     * @return 支付流水游标分页结果
     */
    @GetMapping("/transactions")
    public Result<CursorPageVO<PayTransactionVO>> transactions(@Valid PayPageDTO dto) {
        return Result.success(payService.platformPage(dto).map(PayTransactionVO::from));
    }

    /**
     * 平台资金统计报表，含交易总额、退款总额、交易笔数。
     * @param dto 分页查询入参（复用时间范围字段）
     * @return 统计VO
     */
    @GetMapping("/statistics")
    public Result<PayStatisticsVO> statistics(@Valid PayPageDTO dto) {
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
     * 平台异常支付冻结，将支付单状态置为 FROZEN。
     * @param paymentNo 支付单号
     * @return 操作结果
     */
    @PostMapping("/{paymentNo}/freeze")
    public Result<Void> freeze(@PathVariable("paymentNo") String paymentNo) {
        payService.freeze(paymentNo);
        return Result.success(null);
    }

    /**
     * 平台提现记录游标分页查询。
     * @param dto 游标分页查询入参
     * @return 提现单游标分页结果
     */
    @GetMapping("/withdrawals")
    public Result<CursorPageVO<MerchantWithdrawPO>> withdrawals(@Valid PayPageDTO dto) {
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
