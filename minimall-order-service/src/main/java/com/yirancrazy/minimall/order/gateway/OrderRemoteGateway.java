package com.yirancrazy.minimall.order.gateway;

import com.yirancrazy.minimall.api.dto.cart.CartCheckoutCleanupDto;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDto;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutQueryDto;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDto;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDto;
import com.yirancrazy.minimall.api.dto.stock.StockBatchReserveDto;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDto;
import com.yirancrazy.minimall.common.result.Result;
import java.util.List;
import java.util.Map;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义订单服务访问商品、购物车、库存、支付和 ID 服务的统一边界。
 *   业务边界：接口只暴露跨服务调用结果，不承担本地事务、重试或业务状态更新。
 *   使用场景：OrderServiceImpl 和 CartCleanupServiceImpl 通过该接口隔离 Feign 客户端。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface OrderRemoteGateway {

    /**
     * 读取单个 SKU 的商品快照。
     *
     * <p>通过商品服务获取名称、价格、SPU 和商家信息。
     *
     * @param skuId SKU ID，用于读取商品快照或执行库存操作
     * @return 商品服务统一响应，数据为 SKU 快照。
     */
    Result<SkuSnapshotDto> skuSnapshot(Long skuId);

    /**
     * 批量读取 SKU 快照。
     *
     * <p>按 SKU 集合调用商品服务，用于结算和列表补全。
     *
     * @param skuIds 待批量读取的 SKU ID 列表
     * @return 以 SKU ID 为键的快照映射响应。
     */
    Result<Map<Long, SkuSnapshotDto>> batchSkuSnapshot(List<Long> skuIds);

    /**
     * 批量读取 SPU 快照。
     *
     * <p>用于补充订单明细中的 SPU 标题。
     *
     * @param spuIds 待批量读取的 SPU ID 列表
     * @return 以 SPU ID 为键的快照映射响应。
     */
    Result<Map<Long, SpuSnapshotDto>> batchSpuSnapshot(List<Long> spuIds);

    /**
     * 读取服务端购物车结算快照。
     *
     * <p>购物车服务负责校验条目归属并返回 SKU 和数量。
     *
     * @param dto 购物车结算快照查询参数
     * @return 购物车结算条目列表响应。
     */
    Result<List<CartCheckoutItemDto>> cartCheckoutSnapshot(CartCheckoutQueryDto dto);

    /**
     * 删除已成功结算的购物车条目。
     *
     * <p>清理结果决定任务重试或永久失败。
     *
     * @param dto 购物车清理参数
     * @return 购物车清理统一响应。
     */
    Result<Void> cleanupCheckoutCart(CartCheckoutCleanupDto dto);

    /**
     * 为单个 SKU 预占库存。
     *
     * <p>请求携带订单号作为库存业务幂等键。
     *
     * @param dto 单个 SKU 的库存预占或释放参数
     * @return 库存预占结果响应。
     */
    Result<Boolean> reserve(StockReserveDto dto);

    /**
     * 批量预占同一订单的多个 SKU。
     *
     * <p>库存服务按批量结果整体返回。
     *
     * @param dto 同一订单的批量库存参数
     * @return 批量库存预占结果响应。
     */
    Result<Boolean> reserveBatch(StockBatchReserveDto dto);

    /**
     * 释放订单已占用的库存。
     *
     * <p>使用原订单号作为业务幂等键。
     *
     * @param dto 单个 SKU 的库存预占或释放参数
     * @return 库存释放结果响应。
     */
    Result<Boolean> release(StockReserveDto dto);

    /**
     * 创建订单对应的支付流水。
     *
     * <p>成功时返回支付流水主键，失败由订单服务终止事务。
     *
     * @param dto 支付流水创建参数
     * @return 支付流水 ID 的统一响应。
     */
    Result<Long> createPayment(PayCreateDto dto);

    /**
     * 获取全局业务 ID。
     *
     * <p>bizTag 区分订单和订单组号段。
     *
     * @param bizTag ID 服务的业务号段标识
     * @return 全局 ID 的统一响应。
     */
    Result<Long> nextId(String bizTag);

    /**
     * 提交支付渠道退款。
     *
     * <p>订单状态等待支付回调推进，不在本方法内修改。
     *
     * @param dto 支付退款请求参数
     * @return 退款受理结果响应。
     * @throws BizException 订单不存在、归属不匹配或状态不允许申请退款。
     */
    Result<Boolean> refund(RefundCreateDto dto);
}
