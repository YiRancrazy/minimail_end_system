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
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayStatementVO;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;
import com.yirancrazy.minimall.pay.vo.PayTransactionVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformPayControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/platform/pay")
public class PlatformPayControllerV1 {

    private static final String[] TX_HEADERS = {
        "支付单号", "订单号", "用户ID", "商家ID", "金额", "状态", "创建时间"
    };

    private final PayService payService;

    /**
     * 构造 PlatformPayControllerV1 实例。
     * @param payService payService 参数
     */
    public PlatformPayControllerV1(PayService payService) {
        this.payService = payService;
    }


    /**
     * 执行 transactions 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/transactions")
    public Result<CursorPageVO<PayTransactionVO>> transactions(@Valid PayPageDTO dto) {
        return Result.success(payService.platformPageVO(dto));
    }


    /**
     * 执行 statistics 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/statistics")
    public Result<PayStatisticsVO> statistics(@Valid PayPageDTO dto) {
        return Result.success(payService.statistics(null, dto));
    }


    /**
     * 执行 statement 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/statements")
    public Result<PayStatementVO> statement(@Valid PayStatementDTO dto) {
        return Result.success(payService.statement(dto));
    }


    /**
     * 执行 freeze 方法。
     * @param paymentNo paymentNo 参数
     * @return 统一响应结果
     */
    @PostMapping("/{paymentNo}/freeze")
    public Result<Void> freeze(@PathVariable("paymentNo") String paymentNo) {
        payService.freeze(paymentNo);
        return Result.success(null);
    }


    /**
     * 执行 exportTransactions 方法。
     * @param dto dto 参数
     * @param response response 参数
     * @throws IOException 处理失败时抛出
     */
    public void exportTransactions(@Valid PayPageDTO dto,
                                   HttpServletResponse response) throws IOException {
        List<PayTransactionVO> list = payService.exportTransactionVOs(dto);
        List<String[]> rows = new ArrayList<>(list.size());
        for (PayTransactionVO vo : list) {
            rows.add(new String[] {
                vo.getPaymentNo(),
                vo.getOrderNo(),
                String.valueOf(vo.getUserId()),
                String.valueOf(vo.getMerchantId()),
                vo.getAmount() == null ? "" : vo.getAmount().toPlainString(),
                String.valueOf(vo.getStatus()),
                vo.getCreateTime() == null ? "" : vo.getCreateTime().toString()
            });
        }
        CsvExporter.write(response, "transactions.csv", TX_HEADERS, rows);
    }
}
