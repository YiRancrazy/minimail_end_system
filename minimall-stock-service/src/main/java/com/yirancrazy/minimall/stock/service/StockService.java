package com.yirancrazy.minimall.stock.service;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCompleteDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCreateDTO;
import com.yirancrazy.minimall.stock.dto.StockCountTaskPageDTO;
import com.yirancrazy.minimall.stock.dto.StockPageDTO;
import com.yirancrazy.minimall.stock.dto.StockTransferDTO;
import com.yirancrazy.minimall.stock.dto.StockTransferPageDTO;
import com.yirancrazy.minimall.stock.entity.StockCountTaskPO;
import com.yirancrazy.minimall.stock.entity.StockJournalPO;
import com.yirancrazy.minimall.stock.entity.StockPO;
import com.yirancrazy.minimall.stock.entity.StockTransferPO;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存领域服务接口，定义Stock相关业务契约
 * @Version: 1.1
 * @DateTime: 2026/08/03
 */
public interface StockService {
    /**
     * 预占库存。
     * @param skuId 商品SKU ID
     * @param quantity 预占数量
     * @return 预占是否成功
     */
    boolean reserve(Long skuId, Integer quantity);

    /**
     * 释放库存。
     * @param skuId 商品SKU ID
     * @param quantity 释放数量
     * @return 释放是否成功
     */
    boolean release(Long skuId, Integer quantity);

    /**
     * 查询可用库存。
     * @param skuId 商品SKU ID
     * @return 可用库存数量
     */
    long query(Long skuId);

    /**
     * 设置库存预警阈值。
     * @param skuId 商品SKU ID
     * @param threshold 预警阈值，必须 >= 0
     * @throws com.yirancrazy.minimall.common.exception.BizException 当阈值非法时
     */
    void setThreshold(Long skuId, Long threshold);

    /**
     * 手动调整库存数量。
     * @param skuId 商品SKU ID
     * @param quantity 调整数量，正数增加、负数扣减，不能为0
     * @param reason 调整原因
     * @throws com.yirancrazy.minimall.common.exception.BizException 当调整数量为0时
     */
    void adjustStock(Long skuId, Long quantity, String reason);

    /**
     * 查询指定SKU的库存流水记录。
     * @param skuId 商品SKU ID
     * @return 库存流水列表，按ID降序
     */
    List<StockJournalPO> queryJournal(Long skuId);

    /**
     * 平台分页查询全平台库存，可选按 SKU 过滤或仅查预警库存。
     * @param dto 分页查询入参
     * @return 库存分页结果
     */
    IPage<StockPO> page(StockPageDTO dto);

    /**
     * 全平台库存统计聚合，返回SKU总数、可用/预占总量、预警SKU数及预警比例。
     * @return 库存统计VO
     */
    StockStatisticsVO platformStatistics();

    /**
     * 导出指定SKU的库存流水，最多 10000 行，按ID降序。
     * @param skuId SKU标识
     * @return 库存流水列表
     */
    List<StockJournalPO> exportJournal(Long skuId);

    /**
     * 跨商家库存调拨：扣减源SKU库存、增加目标SKU库存、记录调拨流水与调拨记录。
     * @param dto 调拨入参，含源/目标SKU、数量、原因、操作人
     * @throws com.yirancrazy.minimall.common.exception.BizException 源/目标SKU相同、源库存不足或目标不存在时
     */
    void transfer(StockTransferDTO dto);

    /**
     * 分页查询调拨记录，可选按源/目标SKU过滤，按ID降序返回。
     * @param dto 分页查询入参
     * @return 调拨记录分页结果
     */
    IPage<StockTransferPO> transferPage(StockTransferPageDTO dto);

    /**
     * 下发库存盘点任务，查询当前可用库存作为期望数量。
     * @param dto 创建入参，含SKU与操作人
     * @return 盘点任务ID
     * @throws com.yirancrazy.minimall.common.exception.BizException SKU库存不存在时
     */
    Long createCountTask(StockCountTaskCreateDTO dto);

    /**
     * 分页查询盘点任务，可选按SKU和状态过滤。
     * @param dto 分页查询入参
     * @return 盘点任务分页结果
     */
    IPage<StockCountTaskPO> countTaskPage(StockCountTaskPageDTO dto);

    /**
     * 完成盘点任务，计算差异并调整库存。
     * @param id 任务ID
     * @param dto 完成入参，含实际盘点数量
     * @throws com.yirancrazy.minimall.common.exception.BizException 任务不存在或非待盘点状态时
     */
    void completeCountTask(Long id, StockCountTaskCompleteDTO dto);

    /**
     * 取消盘点任务。
     * @param id 任务ID
     * @param operatorId 操作人ID
     * @throws com.yirancrazy.minimall.common.exception.BizException 任务不存在或非待盘点状态时
     */
    void cancelCountTask(Long id, Long operatorId);
}