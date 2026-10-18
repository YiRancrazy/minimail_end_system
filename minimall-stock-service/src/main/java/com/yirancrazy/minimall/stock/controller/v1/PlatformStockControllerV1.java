package com.yirancrazy.minimall.stock.controller.v1;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.stock.dto.StockCorrectDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCompleteDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCreateDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskPageDTO;
import com.yirancrazy.minimall.stock.dto.StockPageDTO;
import com.yirancrazy.minimall.stock.dto.StockTransferDTO;
import com.yirancrazy.minimall.stock.dto.StockTransferPageDTO;
import com.yirancrazy.minimall.stock.service.StockService;
import com.yirancrazy.minimall.stock.vo.StockCountTaskVO;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVO;
import com.yirancrazy.minimall.stock.vo.StockTransferVO;
import com.yirancrazy.minimall.stock.vo.StockVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformStockControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/platform/stock")
public class PlatformStockControllerV1 {

    private final StockService stockService;

    /**
     * 构造 PlatformStockControllerV1 实例。
     * @param stockService stockService 参数
     */
    public PlatformStockControllerV1(StockService stockService) {
        this.stockService = stockService;
    }


    /**
     * 执行 platformPage 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/page")
    public Result<CursorPageVO<StockVO>> platformPage(@Valid StockPageDTO dto) {
        return Result.success(stockService.pageVO(dto));
    }


    /**
     * 执行 statistics 方法。
     * @return 统一响应结果
     */
    @GetMapping("/statistics")
    public Result<StockStatisticsVO> statistics() {
        return Result.success(stockService.platformStatistics());
    }


    /**
     * 执行 transfer 方法。
     * @param operatorId operatorId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/transfer")
    public Result<Void> transfer(@RequestHeader("X-User-Id") Long operatorId,
                                 @Valid @RequestBody StockTransferDTO dto) {
        dto.setOperatorId(operatorId);
        stockService.transfer(dto);
        return Result.success(null);
    }


    /**
     * 执行 transferPage 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/transfers")
    public Result<CursorPageVO<StockTransferVO>> transferPage(@Valid StockTransferPageDTO dto) {
        return Result.success(stockService.transferPageVO(dto));
    }


    /**
     * 创建业务数据。
     * @param operatorId operatorId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/count-tasks")
    public Result<Long> createCountTask(@RequestHeader("X-User-Id") Long operatorId,
                                        @Valid @RequestBody StockCountTaskCreateDTO dto) {
        dto.setOperatorId(operatorId);
        return Result.success(stockService.createCountTask(dto));
    }


    /**
     * 统计业务数据。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/count-tasks")
    public Result<CursorPageVO<StockCountTaskVO>> countTaskPage(@Valid StockCountTaskPageDTO dto) {
        return Result.success(stockService.countTaskPageVO(dto));
    }


    /**
     * 执行 completeCountTask 方法。
     * @param id id 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/count-tasks/{id}/complete")
    public Result<Void> completeCountTask(@PathVariable Long id,
                                          @Valid @RequestBody StockCountTaskCompleteDTO dto) {
        stockService.completeCountTask(id, dto);
        return Result.success(null);
    }


    /**
     * 执行 cancelCountTask 方法。
     * @param id id 参数
     * @param operatorId operatorId 参数
     * @return 统一响应结果
     */
    @PostMapping("/count-tasks/{id}/cancel")
    public Result<Void> cancelCountTask(@PathVariable Long id,
                                        @RequestHeader("X-User-Id") Long operatorId) {
        stockService.cancelCountTask(id, operatorId);
        return Result.success(null);
    }


    /**
     * 查询列表数据。
     * @return 统一响应结果
     */
    @GetMapping("/abnormal")
    public Result<List<StockVO>> listAbnormal() {
        return Result.success(stockService.listAbnormalStockVO());
    }


    /**
     * 执行 correct 方法。
     * @param skuId skuId 参数
     * @param operatorId operatorId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/{skuId}/correct")
    public Result<Void> correct(@PathVariable Long skuId,
                                @RequestHeader("X-User-Id") Long operatorId,
                                @Valid @RequestBody StockCorrectDTO dto) {
        stockService.correctStock(skuId, dto.getCorrectQuantity(), dto.getReason());
        return Result.success(null);
    }
}
