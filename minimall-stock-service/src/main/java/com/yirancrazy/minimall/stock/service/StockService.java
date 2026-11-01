package com.yirancrazy.minimall.stock.service;

import com.yirancrazy.minimall.api.dto.stock.StockAvailabilityDto;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDto;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCompleteDto;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCreateDto;
import com.yirancrazy.minimall.stock.dto.StockCountTaskPageDto;
import com.yirancrazy.minimall.stock.dto.StockPageDto;
import com.yirancrazy.minimall.stock.dto.StockTransferDto;
import com.yirancrazy.minimall.stock.dto.StockTransferPageDto;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPo;
import com.yirancrazy.minimall.stock.entity.StockJournalPo;
import com.yirancrazy.minimall.stock.entity.StockPo;
import com.yirancrazy.minimall.stock.entity.StockTransferPo;
import com.yirancrazy.minimall.stock.vo.StockCountTaskVo;
import com.yirancrazy.minimall.stock.vo.StockJournalVo;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVo;
import com.yirancrazy.minimall.stock.vo.StockTransferVo;
import com.yirancrazy.minimall.stock.vo.StockVo;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义库存预占、释放、调拨、盘点、纠正及查询的业务契约。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface StockService {

    /**
     * 为指定订单原子预占一个 SKU 的库存，并记录预占流水。
     *
     * @param skuId SKU ID
     * @param quantity 预占数量，必须大于 0
     * @param orderNo 订单号，用于流水追踪和重复释放识别
     * @return 预占成功时返回 true
     * @throws com.yirancrazy.minimall.common.exception.BizException 参数无效、库存不存在或库存不足时抛出
     */
    boolean reserve(Long skuId, Integer quantity, String orderNo);

    /**
     * 导出指定 SKU 最近的库存流水，单次最多返回 10000 条。
     *
     * @param skuId SKU ID
     * @return 按流水 ID 倒序排列的库存流水
     */
    List<StockJournalPo> exportJournal(Long skuId);

    /**
     * 在确认 SKU 归属指定商户后导出其最近库存流水。
     *
     * @param skuId SKU ID
     * @param merchantId 当前商户 ID
     * @return 按流水 ID 倒序排列的库存流水，最多 10000 条
     * @throws com.yirancrazy.minimall.common.exception.BizException SKU 不存在或不属于该商户时抛出
     */
    List<StockJournalPo> exportJournal(Long skuId, Long merchantId);

    /**
     * 批量预占多个 SKU，合并同一 SKU 的明细并在同一事务内逐项执行。
     *
     * @param items 预占明细，最多包含 100 个不同 SKU，同一 SKU 必须使用相同订单号
     * @return 全部预占成功时返回 true
     * @throws com.yirancrazy.minimall.common.exception.BizException 明细为空、参数无效或任一 SKU 库存不足时抛出
     */
    boolean reserveBatch(List<StockReserveDto> items);

    /**
     * 释放指定订单已预占的库存；重复释放由流水唯一键识别为成功。
     *
     * @param skuId SKU ID
     * @param quantity 释放数量
     * @param orderNo 订单号，不能为空
     * @return 释放或识别到重复释放时返回 true；数量不大于 0 时返回 false
     * @throws com.yirancrazy.minimall.common.exception.BizException 订单号为空、SKU 不存在或预占量不足时抛出
     */
    boolean release(Long skuId, Integer quantity, String orderNo);

    /**
     * 为 SKU 创建初始库存；已有记录时保持原值不变。
     *
     * @param skuId SKU ID
     * @param merchantId SKU 所属商户 ID
     * @param initialQuantity 初始可用量；null 或负数按 0 处理
     * @throws com.yirancrazy.minimall.common.exception.BizException SKU ID 为空时抛出
     */
    void initStock(Long skuId, Long merchantId, Integer initialQuantity);

    /**
     * 查询指定 SKU 最近的库存流水，单次最多返回 200 条。
     *
     * @param skuId SKU ID
     * @return 按流水 ID 倒序排列的库存流水
     */
    List<StockJournalPo> queryJournal(Long skuId);

    /**
     * 在确认 SKU 归属指定商户后查询其最近库存流水。
     *
     * @param skuId SKU ID
     * @param merchantId 当前商户 ID
     * @return 最多 200 条库存流水，按 ID 倒序排列
     * @throws com.yirancrazy.minimall.common.exception.BizException SKU 不存在或不属于该商户时抛出
     */
    List<StockJournalPo> queryJournal(Long skuId, Long merchantId);

    /**
     * 查询指定 SKU 的可用量。
     *
     * @param skuId SKU ID
     * @return 可用量；库存记录不存在时返回 0
     */
    long query(Long skuId);

    /**
     * 查询指定商户名下 SKU 的可用量。
     *
     * @param skuId SKU ID
     * @param merchantId 当前商户 ID
     * @return 可用量；库存记录不存在时返回 0
     * @throws com.yirancrazy.minimall.common.exception.BizException 库存记录存在但不属于该商户时抛出
     */
    long query(Long skuId, Long merchantId);

    /**
     * 批量查询 SKU 可用库存，过滤空值并去重，单次最多处理 500 个 SKU。
     *
     * @param skuIds SKU ID 列表
     * @return 按去重后输入顺序返回快照，不存在的 SKU 可用量为 0
     */
    List<StockAvailabilityDto> queryAvailability(List<Long> skuIds);

    /**
     * 设置未绑定商户校验的 SKU 库存告警阈值；记录不存在时创建零库存记录。
     *
     * @param skuId SKU ID
     * @param threshold 告警阈值，必须大于等于 0
     * @throws com.yirancrazy.minimall.common.exception.BizException 阈值无效或并发更新失败时抛出
     */
    void setThreshold(Long skuId, Long threshold);

    /**
     * 在创建或修改库存前校验商户归属，并设置库存告警阈值。
     *
     * @param skuId SKU ID
     * @param threshold 告警阈值，必须大于等于 0
     * @param merchantId 当前商户 ID
     * @throws com.yirancrazy.minimall.common.exception.BizException 阈值无效、商户不匹配或并发更新失败时抛出
     */
    void setThreshold(Long skuId, Long threshold, Long merchantId);

    /**
     * 按增量调整 SKU 可用量，记录调整流水，并在低于阈值时输出告警日志。
     *
     * @param skuId SKU ID
     * @param quantity 调整增量，必须非 0；负数不得使可用量小于 0
     * @param reason 调整原因，可为空
     * @throws com.yirancrazy.minimall.common.exception.BizException 调整量为 0、库存不足或并发更新失败时抛出
     */
    void adjustStock(Long skuId, Long quantity, String reason);

    /**
     * 在商户归属校验后按增量调整 SKU 可用量并记录流水。
     *
     * @param skuId SKU ID
     * @param quantity 调整增量，必须非 0；负数不得使可用量小于 0
     * @param reason 调整原因，可为空
     * @param merchantId 当前商户 ID
     * @throws com.yirancrazy.minimall.common.exception.BizException 调整量无效、商户不匹配、库存不足或并发更新失败时抛出
     */
    void adjustStock(Long skuId, Long quantity, String reason, Long merchantId);

    /**
     * 校验商户归属后查询库存流水并转换为接口视图。
     *
     * @param skuId SKU ID
     * @param merchantId 当前商户 ID
     * @return 最多 200 条库存流水视图
     * @throws com.yirancrazy.minimall.common.exception.BizException SKU 不存在或不属于该商户时抛出
     */
    List<StockJournalVo> queryJournalVo(Long skuId, Long merchantId);

    /**
     * 使用 ID 游标分页查询库存记录，可按 SKU 和低库存状态筛选。
     *
     * @param dto 游标、分页大小和筛选条件
     * @return 按库存 ID 倒序排列的游标分页结果
     */
    CursorPageVo<StockPo> page(StockPageDto dto);

    /**
     * 使用 ID 游标分页查询库存记录并转换为接口视图。
     *
     * @param dto 游标、分页大小和筛选条件
     * @return 按库存 ID 倒序排列的库存视图分页结果
     */
    CursorPageVo<StockVo> pageVo(StockPageDto dto);

    /**
     * 查询可用量或预占量为负的异常库存记录。
     *
     * @return 最多 200 条异常库存，未额外排序
     */
    List<StockPo> listAbnormalStock();

    /**
     * 汇总平台库存指标，并以 SKU 数为基数计算两位小数的告警占比。
     *
     * @return 库存统计视图；聚合结果为空时返回全零指标
     */
    StockStatisticsVo platformStatistics();

    /**
     * 校验商户归属后导出库存流水并转换为接口视图。
     *
     * @param skuId SKU ID
     * @param merchantId 当前商户 ID
     * @return 最多 10000 条库存流水视图
     * @throws com.yirancrazy.minimall.common.exception.BizException SKU 不存在或不属于该商户时抛出
     */
    List<StockJournalVo> exportJournalVo(Long skuId, Long merchantId);

    /**
     * 在同一事务内将可用库存从源 SKU 调拨到目标 SKU，并保存流水与调拨记录。
     *
     * @param dto 源 SKU、目标 SKU、数量和操作审计信息
     * @throws com.yirancrazy.minimall.common.exception.BizException 源目标相同、源库存不足或目标记录不存在时抛出
     */
    void transfer(StockTransferDto dto);

    /**
     * 使用 ID 游标分页查询调拨审计记录，可按源、目标 SKU 筛选。
     *
     * @param dto 游标、分页大小和筛选条件
     * @return 按调拨记录 ID 倒序排列的游标分页结果
     */
    CursorPageVo<StockTransferPo> transferPage(StockTransferPageDto dto);

    /**
     * 使用 ID 游标分页查询调拨记录并转换为接口视图。
     *
     * @param dto 游标、分页大小和筛选条件
     * @return 按调拨记录 ID 倒序排列的视图分页结果
     */
    CursorPageVo<StockTransferVo> transferPageVo(StockTransferPageDto dto);

    /**
     * 使用 ID 游标分页查询盘点任务，可按 SKU 和状态筛选。
     *
     * @param dto 游标、分页大小和筛选条件
     * @return 按盘点任务 ID 倒序排列的游标分页结果
     */
    CursorPageVo<StockCountTaskPo> countTaskPage(StockCountTaskPageDto dto);

    /**
     * 基于 SKU 当前可用量创建待盘点任务。
     *
     * @param dto SKU、备注和操作人信息
     * @return 新建盘点任务 ID
     * @throws com.yirancrazy.minimall.common.exception.BizException SKU 库存记录不存在时抛出
     */
    Long createCountTask(StockCountTaskCreateDto dto);

    /**
     * 使用 ID 游标分页查询盘点任务并转换为接口视图。
     *
     * @param dto 游标、分页大小和筛选条件
     * @return 按盘点任务 ID 倒序排列的视图分页结果
     */
    CursorPageVo<StockCountTaskVo> countTaskPageVo(StockCountTaskPageDto dto);

    /**
     * 完成待盘点任务；存在实盘差异时在同一事务内调整库存。
     *
     * @param id 盘点任务 ID
     * @param dto 实盘数量和可选备注
     * @throws com.yirancrazy.minimall.common.exception.BizException 任务不存在、状态非待盘点或并发更新失败时抛出
     */
    void completeCountTask(Long id, StockCountTaskCompleteDto dto);

    /**
     * 取消待盘点任务，不修改库存数量。
     *
     * @param id 盘点任务 ID
     * @param operatorId 操作人 ID，仅用于日志审计
     * @throws com.yirancrazy.minimall.common.exception.BizException 任务不存在、状态非待盘点或并发更新失败时抛出
     */
    void cancelCountTask(Long id, Long operatorId);

    /**
     * 查询异常库存并转换为接口视图。
     *
     * @return 最多 200 条可用量或预占量为负的库存视图
     */
    List<StockVo> listAbnormalStockVo();

    /**
     * 将 SKU 可用量直接纠正为目标值，并按新旧差值记录纠正流水。
     *
     * @param skuId SKU ID
     * @param correctQuantity 目标可用量，必须大于等于 0
     * @param reason 纠正原因，可为空
     * @throws com.yirancrazy.minimall.common.exception.BizException 目标值无效、库存不存在或并发更新失败时抛出
     */
    void correctStock(Long skuId, Long correctQuantity, String reason);
}
