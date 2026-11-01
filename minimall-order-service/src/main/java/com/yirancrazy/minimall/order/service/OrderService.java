package com.yirancrazy.minimall.order.service;

import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDto;
import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDto;
import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDto;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDto;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.order.dto.CheckoutResultDto;
import com.yirancrazy.minimall.order.dto.OrderCheckoutItemDto;
import com.yirancrazy.minimall.order.dto.OrderPageDto;
import com.yirancrazy.minimall.order.dto.ReceiverDto;
import com.yirancrazy.minimall.order.entity.OrderPo;
import com.yirancrazy.minimall.order.vo.OrderExportVo;
import com.yirancrazy.minimall.order.vo.OrderItemVo;
import com.yirancrazy.minimall.order.vo.OrderLogisticsVo;
import com.yirancrazy.minimall.order.vo.OrderStatisticsVo;
import com.yirancrazy.minimall.order.vo.OrderStatusCountsVo;
import com.yirancrazy.minimall.order.vo.OrderSummaryVo;
import com.yirancrazy.minimall.order.vo.OrderTrendVo;
import com.yirancrazy.minimall.order.vo.OrderVo;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义订单生命周期、退款、查询、统计、导出和补偿扫描的业务契约。
 *   业务边界：接口覆盖用户、商家、平台与内部调用，具体事务、状态机、远程调用和幂等语义由实现类承担。
 *   使用场景：各订单控制器、结算编排和定时任务依赖该服务。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface OrderService {
    /**
     * 创建单个 SKU 订单并完成库存预占和支付流水初始化。
     *
     * <p>读取服务端 SKU 快照，按最终订单号预占库存，保存订单及明细快照，再创建支付流水；四参数版本把收货信息写入快照。该方法处于全局事务边界内。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param skuId SKU ID，用于读取商品快照或执行库存操作
     * @param quantity 购买数量，必须为正整数
     * @return 新订单主键。
     * @throws BizException SKU 快照缺失、库存不足、ID 生成失败或支付流水创建失败。
     */
    Long create(Long userId, Long skuId, Integer quantity);

    /**
     * 创建单个 SKU 订单并完成库存预占和支付流水初始化。
     *
     * <p>读取服务端 SKU 快照，按最终订单号预占库存，保存订单及明细快照，再创建支付流水；四参数版本把收货信息写入快照。该方法处于全局事务边界内。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param skuId SKU ID，用于读取商品快照或执行库存操作
     * @param quantity 购买数量，必须为正整数
     * @param receiver 收货信息快照
     * @return 新订单主键。
     * @throws BizException SKU 快照缺失、库存不足、ID 生成失败或支付流水创建失败。
     */
    Long create(Long userId, Long skuId, Integer quantity, ReceiverDto receiver);

    /**
     * 创建一次结算订单并返回订单、订单组和购物车清理任务标识。
     *
     * <p>合并重复 SKU、限制最多 100 个 SKU、校验所有 SKU 属于同一商家、批量预占库存并保存明细快照；存在购物车条目时创建异步清理任务。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param items 订单 SKU 与数量明细
     * @return 订单主键或包含清理任务 ID 的结算结果。
     * @throws BizException 条目为空或非法、跨商家、快照缺失、库存不足、ID 或支付流水创建失败。
     */
    Long checkout(Long userId, List<OrderCheckoutItemDto> items);

    /**
     * 创建一次结算订单并返回订单、订单组和购物车清理任务标识。
     *
     * <p>合并重复 SKU、限制最多 100 个 SKU、校验所有 SKU 属于同一商家、批量预占库存并保存明细快照；存在购物车条目时创建异步清理任务。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param items 订单 SKU 与数量明细
     * @param receiver 收货信息快照
     * @return 订单主键或包含清理任务 ID 的结算结果。
     * @throws BizException 条目为空或非法、跨商家、快照缺失、库存不足、ID 或支付流水创建失败。
     */
    Long checkout(Long userId, List<OrderCheckoutItemDto> items, ReceiverDto receiver);

    /**
     * 创建一次结算订单并返回订单、订单组和购物车清理任务标识。
     *
     * <p>合并重复 SKU、限制最多 100 个 SKU、校验所有 SKU 属于同一商家、批量预占库存并保存明细快照；存在购物车条目时创建异步清理任务。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param items 订单 SKU 与数量明细
     * @param cleanupItems 结算成功后待清理的购物车条目快照
     * @param receiver 收货信息快照
     * @return 订单主键或包含清理任务 ID 的结算结果。
     * @throws BizException 条目为空或非法、跨商家、快照缺失、库存不足、ID 或支付流水创建失败。
     */
    CheckoutResultDto checkout(
            Long userId,
            List<OrderCheckoutItemDto> items,
            List<CartCheckoutItemDto> cleanupItems,
            ReceiverDto receiver);

    /**
     * 把待支付订单推进为已支付并写入支付成功事件。
     *
     * <p>重复支付请求对已支付订单直接返回；其他非待支付状态拒绝。状态更新和发件箱写入位于同一事务。
     *
     * @param orderId 订单 ID
     * @throws BizException 订单不存在或状态不允许支付。
     */
    void pay(Long orderId);

    /**
     * 按订单号定位订单后执行支付状态推进。
     *
     * <p>先按 orderNo 查询唯一订单，再复用 pay 的幂等和事务逻辑。
     *
     * @param orderNo 业务订单号
     * @throws BizException 订单号为空、订单不存在或状态不允许支付。
     */
    void payByOrderNo(String orderNo);

    /**
     * 由用户取消订单并释放已预占库存。
     *
     * <p>先校验订单归属和状态，再记录库存待释放状态、推进为已取消并调用库存服务；释放失败保留待补偿状态。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @throws BizException 订单不存在、归属不匹配或当前状态不允许取消。
     */
    void cancel(Long orderId, Long userId);

    /**
     * 由商家完成发货并写入物流节点。
     *
     * <p>校验订单归属商家、推进为已发货，并保存承运商和运单号组成的物流描述。
     *
     * @param orderId 订单 ID
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param carrier 承运商名称
     * @param trackingNo 物流运单号
     * @throws BizException 订单不存在、商家不匹配或订单状态不允许发货。
     */
    void ship(Long orderId, Long merchantId, String carrier, String trackingNo);

    /**
     * 由用户确认收货并把已发货订单推进为已完成。
     *
     * <p>校验订单归属后更新状态、收货时间并写入状态日志。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @throws BizException 订单不存在、归属不匹配或状态不允许确认。
     */
    void confirm(Long orderId, Long userId);

    /**
     * 登记用户退款申请并进入退款中状态。
     *
     * <p>只保存退款来源状态和稳定退款请求号，不直接调用支付网关；真实退款必须经过商家审核或平台仲裁。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @throws BizException 订单不存在、归属不匹配或当前状态不允许申请退款。
     */
    void refund(Long orderId, Long userId);

    /**
     * 由商家发起指定金额的退款申请。
     *
     * <p>校验商家归属、当前购买态和退款金额，只登记退款中状态；默认退款请求号由订单 ID 派生。
     *
     * @param orderId 订单 ID
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param refundAmount 退款金额字符串，最多两位小数且不能超过剩余可退金额
     * @param reason 退款、关闭或状态日志说明
     * @throws BizException 商家不匹配、状态不允许、金额非法或既有退款请求号冲突。
     */
    void merchantInitiateRefund(Long orderId, Long merchantId, String refundAmount, String reason);

    /**
     * 由商家发起指定金额的退款申请。
     *
     * <p>校验商家归属、当前购买态和退款金额，只登记退款中状态；默认退款请求号由订单 ID 派生。
     *
     * @param orderId 订单 ID
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param refundAmount 退款金额字符串，最多两位小数且不能超过剩余可退金额
     * @param reason 退款、关闭或状态日志说明
     * @param refundRequestNo 稳定退款请求号，用于支付侧幂等
     * @throws BizException 商家不匹配、状态不允许、金额非法或既有退款请求号冲突。
     */
    void merchantInitiateRefund(
            Long orderId, Long merchantId, String refundAmount, String reason, String refundRequestNo);

    /**
     * 按退款单号解析订单后发起商家退款。
     *
     * <p>退款单号可带 R 前缀；解析得到的订单 ID 会继续走商家退款校验和状态推进。
     *
     * @param refundNo 退款单号，可带 R 前缀
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param refundAmount 退款金额字符串，最多两位小数且不能超过剩余可退金额
     * @param reason 退款、关闭或状态日志说明
     * @throws BizException 退款单号为空、格式错误或退款申请非法。
     */
    void merchantInitiateRefundByNo(
            String refundNo, Long merchantId, String refundAmount, String reason);

    /**
     * 处理支付侧退款回调并推进订单退款结果。
     *
     * <p>校验支付单号、退款请求号和累计金额；重复累计金额直接忽略，全额退款进入已退款，部分退款回退到退款前状态并清理本轮请求键。
     *
     * @param dto 支付退款回调数据
     * @throws BizException 回调字段缺失、金额不一致、支付单或请求号冲突、状态非退款中。
     */
    void handleRefundCallback(RefundCallbackDto dto);

    /**
     * 判断用户是否可就指定 SPU 评价订单。
     *
     * <p>要求订单归属用户、状态属于已支付/已发货/已完成且订单明细包含该 SPU；商家优先取明细快照。
     *
     * @param orderNo 业务订单号
     * @param userId 用户 ID，必须对应当前订单归属
     * @param spuId SPU ID
     * @return 包含资格、订单、用户、商家和订单状态的快照对象；非法输入返回不符合条件。
     */
    OrderCommentEligibilityDto commentEligibility(String orderNo, Long userId, Long spuId);

    /**
     * 查询订单详情并可按用户身份校验归属。
     *
     * <p>带 userId 的重载在订单不存在或归属不匹配时按订单不存在处理；单参数版本供可信内部链路使用。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 订单持久化对象。
     * @throws BizException 订单不存在或用户不匹配。
     */
    OrderPo getDetail(Long orderId, Long userId);

    /**
     * 查询订单详情并可按用户身份校验归属。
     *
     * <p>带 userId 的重载在订单不存在或归属不匹配时按订单不存在处理；单参数版本供可信内部链路使用。
     *
     * @param orderId 订单 ID
     * @return 订单持久化对象。
     * @throws BizException 订单不存在或用户不匹配。
     */
    OrderPo getDetail(Long orderId);

    /**
     * 读取订单状态码。
     *
     * <p>带 userId 的重载校验订单归属，单参数版本供内部可信调用；不修改订单。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 订单状态整数编码。
     * @throws BizException 订单不存在或用户不匹配。
     */
    Integer getStatus(Long orderId, Long userId);

    /**
     * 读取订单状态码。
     *
     * <p>带 userId 的重载校验订单归属，单参数版本供内部可信调用；不修改订单。
     *
     * @param orderId 订单 ID
     * @return 订单状态整数编码。
     * @throws BizException 订单不存在或用户不匹配。
     */
    Integer getStatus(Long orderId);

    /**
     * 按订单号或数字订单 ID 解析订单归属商家。
     *
     * <p>先按 orderNo 精确查询，未命中且 ref 为纯数字时再按主键查询；不存在时返回 null。
     *
     * @param ref 订单号或数字订单 ID
     * @return 商家 ID，或订单不存在时的 null。
     */
    Long resolveMerchantId(String ref);

    /**
     * 查询支付服务所需的订单支付快照。
     *
     * <p>按订单号或数字订单 ID 定位订单，返回支付金额、状态、用户、商家和支付流水号。
     *
     * @param ref 订单号或数字订单 ID
     * @return 订单支付快照。
     * @throws BizException 订单不存在。
     */
    OrderPaySnapshotDto getPaySnapshot(String ref);

    /**
     * 查询订单明细并组装展示对象。
     *
     * <p>批量解析 SKU 图片公开地址并补充 SPU 名称；订单无明细时返回空列表。
     *
     * @param orderId 订单 ID
     * @return 订单明细视图列表。
     */
    List<OrderItemVo> listItemVo(Long orderId);

    /**
     * 由商家关闭订单。
     *
     * <p>商家身份来自请求头，关闭和库存释放规则由 OrderService 执行。
     *
     * @param orderId 订单 ID
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @return 空数据的统一响应。
     * @throws BizException 商家不匹配或订单状态不允许关闭。
     */
    void merchantClose(Long orderId, Long merchantId);

    /**
     * 删除满足终态的订单记录。
     *
     * <p>只允许用户删除自己的已取消、已完成或已退款订单，执行逻辑删除；其他状态拒绝。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @throws BizException 订单不存在、归属不匹配或状态不允许删除。
     */
    void delete(Long orderId, Long userId);

    /**
     * 由平台关闭订单并触发库存释放。
     *
     * <p>推进订单为已取消，记录平台关闭原因并尝试释放库存；释放失败保留待补偿状态。
     *
     * @param orderId 订单 ID
     * @throws BizException 订单不存在或状态不允许平台关闭。
     */
    void platformClose(Long orderId);

    /**
     * 查询商家待办订单数量。
     *
     * <p>统计待支付、已支付和退款中状态，不包含终态订单。
     *
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @return 订单数量统一响应。
     */
    long pendingCount(Long merchantId);

    /**
     * 查询订单游标分页。
     *
     * <p>强制使用请求头身份作为筛选条件，并将分页参数转交订单服务。
     *
     * @param dto 订单分页和筛选参数
     * @return 订单视图游标页。
     */
    CursorPageVo<OrderPo> page(OrderPageDto dto);

    /**
     * 查询订单游标页并转换为展示对象。
     *
     * <p>复用 page 的筛选和游标语义，不额外补充明细。
     *
     * @param dto 订单分页和筛选参数
     * @return 订单视图游标页。
     */
    CursorPageVo<OrderVo> pageVo(OrderPageDto dto);

    /**
     * 查询用户订单详情并补充明细。
     *
     * <p>先校验订单归属，再转换主记录并加载订单明细视图。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 包含明细的订单视图。
     * @throws BizException 订单不存在或用户不匹配。
     */
    OrderVo detailVo(Long orderId, Long userId);

    /**
     * 查询平台订单详情。
     *
     * <p>按订单 ID 读取主记录并转换为视图，不额外加载明细列表。
     *
     * @param orderId 订单 ID
     * @return 订单视图。
     * @throws BizException 订单不存在。
     */
    OrderVo platformDetailVo(Long orderId);

    /**
     * 按平台筛选条件查询 CSV 导出订单。
     *
     * <p>可叠加用户、商家、状态和时间范围，结果按创建时间倒序并限制最大行数。
     *
     * @param dto 订单分页和筛选参数
     * @return 待导出的订单主记录列表。
     */
    List<OrderPo> platformExportList(OrderPageDto dto);

    /**
     * 查询商家订单游标页并补全列表展示字段。
     *
     * <p>批量填充明细汇总数量、SKU/SPU 展示名称和最近一次退款拒绝原因。
     *
     * @param dto 订单分页和筛选参数
     * @return 补全后的商家订单游标页。
     */
    CursorPageVo<OrderVo> merchantPageVo(OrderPageDto dto);

    /**
     * 查询指定商家的订单详情并补全展示字段。
     *
     * <p>订单必须同时匹配订单 ID 和商家 ID；随后填充明细汇总、SKU 名称和退款拒绝原因。
     *
     * @param orderId 订单 ID
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @return 补全后的订单视图。
     * @throws BizException 订单不存在或不属于该商家。
     */
    OrderVo merchantDetailVo(Long orderId, Long merchantId);

    /**
     * 审核退款中的商家订单。
     *
     * <p>驳回时回退到退款前状态并写入原因；通过时调用支付退款，订单状态由支付回调最终推进。退款请求号必须稳定一致。
     *
     * @param orderId 订单 ID
     * @param approved true 表示通过，false 表示驳回
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param reason 退款、关闭或状态日志说明
     * @throws BizException 商家不匹配、订单非退款中、退款请求号冲突或支付退款失败。
     */
    void reviewRefund(Long orderId, boolean approved, Long merchantId, String reason);

    /**
     * 审核退款中的商家订单。
     *
     * <p>驳回时回退到退款前状态并写入原因；通过时调用支付退款，订单状态由支付回调最终推进。退款请求号必须稳定一致。
     *
     * @param orderId 订单 ID
     * @param approved true 表示通过，false 表示驳回
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param reason 退款、关闭或状态日志说明
     * @param refundRequestNo 稳定退款请求号，用于支付侧幂等
     * @throws BizException 商家不匹配、订单非退款中、退款请求号冲突或支付退款失败。
     */
    void reviewRefund(
            Long orderId, boolean approved, Long merchantId, String reason, String refundRequestNo);

    /**
     * 按退款单号审核商家退款。
     *
     * <p>从 R 前缀或纯数字退款单号解析订单 ID，再复用退款审核逻辑。
     *
     * @param refundNo 退款单号，可带 R 前缀
     * @param approved true 表示通过，false 表示驳回
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param reason 退款、关闭或状态日志说明
     * @throws BizException 退款单号非法、商家不匹配或退款状态不允许审核。
     */
    void reviewRefundByNo(String refundNo, boolean approved, Long merchantId, String reason);

    /**
     * 由平台仲裁退款申请。
     *
     * <p>同意时直接提交支付退款并保持退款中等待回调；拒绝时回退到退款前状态。
     *
     * @param orderId 订单 ID
     * @param approved true 表示通过，false 表示驳回
     * @throws BizException 订单非退款中或支付退款失败。
     */
    void arbitrateRefund(Long orderId, boolean approved);

    /**
     * 查询用户订单的物流节点。
     *
     * <p>校验订单归属后按创建时间升序转换节点，不调用外部物流服务。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 按时间排序的物流节点列表。
     * @throws BizException 订单不存在或用户不匹配。
     */
    List<OrderLogisticsVo> queryLogistics(Long orderId, Long userId);

    /**
     * 按商家和筛选条件查询 CSV 导出订单。
     *
     * <p>结果按创建时间倒序并受 CsvExporter 最大导出行数限制。
     *
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param dto 订单分页和筛选参数
     * @return 待导出的订单主记录列表。
     */
    List<OrderPo> exportList(Long merchantId, OrderPageDto dto);

    /**
     * 按商家筛选并转换为 CSV 导出视图。
     *
     * <p>复用 exportList 的查询和行数限制。
     *
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @param dto 订单分页和筛选参数
     * @return 待导出的订单视图列表。
     */
    List<OrderExportVo> exportVoList(Long merchantId, OrderPageDto dto);

    /**
     * 按平台筛选条件转换 CSV 导出视图。
     *
     * <p>接受分页 DTO 或起止日期字符串；日期参数非法时由解析方法降级为无边界。
     *
     * @param dto 订单分页和筛选参数
     * @return 待导出的订单视图列表。
     */
    List<OrderExportVo> platformExportVoList(OrderPageDto dto);

    /**
     * 按平台筛选条件转换 CSV 导出视图。
     *
     * <p>接受分页 DTO 或起止日期字符串；日期参数非法时由解析方法降级为无边界。
     *
     * @param startDate 可选的 yyyy-MM-dd 开始日期
     * @param endDate 可选的 yyyy-MM-dd 结束日期
     * @return 待导出的订单视图列表。
     */
    List<OrderExportVo> platformExportVoList(String startDate, String endDate);

    /**
     * 执行订单统计 SQL。
     *
     * <p>按商家和时间范围聚合单量、支付金额、退款金额及分状态计数。
     *
     * @param dto 订单分页和筛选参数
     * @return 订单统计数据库投影。
     */
    OrderStatisticsVo statistics(OrderPageDto dto);

    /**
     * 汇总指定日期和商家的订单经营指标。
     *
     * <p>日期参数非法时忽略对应边界，merchantId 非法时按全平台汇总。
     *
     * @param startDate 可选的 yyyy-MM-dd 开始日期
     * @param endDate 可选的 yyyy-MM-dd 结束日期
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @return 订单汇总视图或 null。
     */
    OrderSummaryVo orderSummary(String startDate, String endDate, String merchantId);

    /**
     * 按日期汇总订单单量和金额趋势。
     *
     * <p>日期或商家参数非法时降级为对应维度无筛选，无数据时返回空点位列表。
     *
     * @param startDate 可选的 yyyy-MM-dd 开始日期
     * @param endDate 可选的 yyyy-MM-dd 结束日期
     * @param merchantId 商家 ID，用于订单归属或统计范围校验
     * @return 订单趋势视图。
     */
    OrderTrendVo orderTrend(String startDate, String endDate, String merchantId);

    /**
     * 把用户订单状态分组计数转换为展示对象。
     *
     * <p>只映射 1 到 7 的已定义状态，缺失状态保持默认 0。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 用户订单状态计数视图。
     */
    OrderStatusCountsVo countStatusByUser(Long userId);

    /**
     * 由调度器触发支付超时订单扫描。
     *
     * <p>使用 Redis 锁保证单实例执行，业务处理委托 OrderService。
     *
     * @return 本次扫描或处理的数量。
     */
    int scanExpiredOrders();

    /**
     * 由调度器触发已发货订单自动确认。
     *
     * <p>使用独立 Redis 锁，避免多实例重复推进订单状态。
     *
     * @return 本次扫描或处理的数量。
     */
    int scanAutoConfirm();

    /**
     * 由调度器触发待释放库存补偿。
     *
     * <p>使用独立 Redis 锁并按批扫描，将具体释放逻辑交给 OrderService。
     *
     * @return 本次扫描或处理的数量。
     */
    int scanStockReleasePending();
}
