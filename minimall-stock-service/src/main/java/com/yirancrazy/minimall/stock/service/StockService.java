package com.yirancrazy.minimall.stock.service;

import java.util.List;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.common.result.CursorPageVO;
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
import com.yirancrazy.minimall.stock.vo.StockCountTaskVO;
import com.yirancrazy.minimall.stock.vo.StockJournalVO;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVO;
import com.yirancrazy.minimall.stock.vo.StockTransferVO;
import com.yirancrazy.minimall.stock.vo.StockVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: StockService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface StockService {

    /**
     * 执行 reserve 方法。
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @return 是否处理成功
     */
    boolean reserve(Long skuId, Integer quantity);


    /**
     * 执行 exportJournal 方法。
     * @param skuId skuId 参数
     * @return 结果列表
     */
    List<StockJournalPO> exportJournal(Long skuId);
    /**
     * 执行 reserveBatch 方法。
     * @param items items 参数
     * @return 是否处理成功
     */
    boolean reserveBatch(List<StockReserveDTO> items);


    /**
     * 执行 release 方法。
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @return 是否处理成功
     */
    default boolean release(Long skuId, Integer quantity) {
        return release(skuId, quantity, null);
    }


    /**
     * 执行 release 方法。
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @param orderNo orderNo 参数
     * @return 是否处理成功
     */
    boolean release(Long skuId, Integer quantity, String orderNo);


    /**
     * 初始化相关资源。
     * @param skuId skuId 参数
     * @param merchantId merchantId 参数
     * @param initialQuantity initialQuantity 参数
     */
    void initStock(Long skuId, Long merchantId, Integer initialQuantity);


    /**
     * 执行 queryJournal 方法。
     * @param skuId skuId 参数
     * @return 结果列表
     */
    List<StockJournalPO> queryJournal(Long skuId);
    /**
     * 执行 query 方法。
     * @param skuId skuId 参数
     * @return 业务数值
     */
    long query(Long skuId);


    /**
     * 执行 queryJournal 方法。
     * @param skuId skuId 参数
     * @param merchantId merchantId 参数
     * @return 结果列表
     */
    List<StockJournalPO> queryJournal(Long skuId, Long merchantId);
    /**
     * 执行 query 方法。
     * @param skuId skuId 参数
     * @param merchantId merchantId 参数
     * @return 业务数值
     */
    long query(Long skuId, Long merchantId);


    /**
     * 设置相关数据。
     * @param skuId skuId 参数
     * @param threshold threshold 参数
     */
    void setThreshold(Long skuId, Long threshold);


    /**
     * 设置相关数据。
     * @param skuId skuId 参数
     * @param threshold threshold 参数
     * @param merchantId merchantId 参数
     */
    void setThreshold(Long skuId, Long threshold, Long merchantId);


    /**
     * 执行 adjustStock 方法。
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @param reason reason 参数
     */
    void adjustStock(Long skuId, Long quantity, String reason);


    /**
     * 执行 adjustStock 方法。
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @param reason reason 参数
     * @param merchantId merchantId 参数
     */
    void adjustStock(Long skuId, Long quantity, String reason, Long merchantId);




    /**
     * 执行 exportJournal 方法。
     * @param skuId skuId 参数
     * @param merchantId merchantId 参数
     * @return 结果列表
     */
    List<StockJournalPO> exportJournal(Long skuId, Long merchantId);
    /**
     * 执行 queryJournalVO 方法。
     * @param skuId skuId 参数
     * @param merchantId merchantId 参数
     * @return 结果列表
     */
    List<StockJournalVO> queryJournalVO(Long skuId, Long merchantId);



    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<StockPO> page(StockPageDTO dto);
    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<StockVO> pageVO(StockPageDTO dto);


    /**
     * 查询列表数据。
     * @return 结果列表
     */
    List<StockPO> listAbnormalStock();
    /**
     * 执行 platformStatistics 方法。
     * @return 处理结果
     */
    StockStatisticsVO platformStatistics();




    /**
     * 执行 exportJournalVO 方法。
     * @param skuId skuId 参数
     * @param merchantId merchantId 参数
     * @return 结果列表
     */
    List<StockJournalVO> exportJournalVO(Long skuId, Long merchantId);


    /**
     * 执行 transfer 方法。
     * @param dto dto 参数
     */
    void transfer(StockTransferDTO dto);



    /**
     * 执行 transferPage 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<StockTransferPO> transferPage(StockTransferPageDTO dto);
    /**
     * 执行 transferPageVO 方法。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<StockTransferVO> transferPageVO(StockTransferPageDTO dto);


    /**
     * 统计业务数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<StockCountTaskPO> countTaskPage(StockCountTaskPageDTO dto);
    /**
     * 创建业务数据。
     * @param dto dto 参数
     * @return 业务数值
     */
    Long createCountTask(StockCountTaskCreateDTO dto);



    /**
     * 统计业务数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<StockCountTaskVO> countTaskPageVO(StockCountTaskPageDTO dto);


    /**
     * 执行 completeCountTask 方法。
     * @param id id 参数
     * @param dto dto 参数
     */
    void completeCountTask(Long id, StockCountTaskCompleteDTO dto);


    /**
     * 执行 cancelCountTask 方法。
     * @param id id 参数
     * @param operatorId operatorId 参数
     */
    void cancelCountTask(Long id, Long operatorId);



    /**
     * 查询列表数据。
     * @return 结果列表
     */
    List<StockVO> listAbnormalStockVO();


    /**
     * 执行 correctStock 方法。
     * @param skuId skuId 参数
     * @param correctQuantity correctQuantity 参数
     * @param reason reason 参数
     */
    void correctStock(Long skuId, Long correctQuantity, String reason);
}
