package com.yirancrazy.minimall.order.controller.v1;

import com.yirancrazy.minimall.common.annotation.Idempotency;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.dto.OrderCheckoutDto;
import com.yirancrazy.minimall.order.dto.OrderCreateDto;
import com.yirancrazy.minimall.order.dto.OrderPageDto;
import com.yirancrazy.minimall.order.handle.OrderCheckoutReplayHandler;
import com.yirancrazy.minimall.order.service.OrderCheckoutService;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderLogisticsVo;
import com.yirancrazy.minimall.order.vo.OrderStatusCountsVo;
import com.yirancrazy.minimall.order.vo.OrderVo;
import jakarta.validation.Valid;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供用户端订单分页、状态统计、结算、详情、取消、退款、删除和物流查询接口。
 *   业务边界：用户身份取自请求头并覆盖客户端筛选值，业务规则委托 OrderService 和 OrderCheckoutService。
 *   使用场景：用户订单中心通过 /api/v1/user/orders 调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/user/orders")
public class UserOrderControllerV1 {

    private final OrderService orderService;
    private final OrderCheckoutService orderCheckoutService;

    /**
     * 创建用户订单控制器并注入订单与结算服务。
     *
     * <p>普通订单操作委托 OrderService，结算来源解析委托 OrderCheckoutService。
     *
     * @param orderService 订单业务服务
     * @param orderCheckoutService 订单结算编排服务
     */
    public UserOrderControllerV1(
            OrderService orderService, OrderCheckoutService orderCheckoutService) {
        this.orderService = orderService;
        this.orderCheckoutService = orderCheckoutService;
    }

    /**
     * 查询当前用户订单游标页。
     *
     * <p>请求头用户 ID 会覆盖客户端 userId 筛选值。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param dto 订单分页和筛选参数
     * @return 订单视图游标页统一响应。
     */
    @GetMapping
    public Result<CursorPageVo<OrderVo>> page(
            @RequestHeader("X-User-Id") Long userId, @Valid OrderPageDto dto) {
        dto.setUserId(userId);
        return Result.success(orderService.pageVo(dto));
    }

    /**
     * 查询当前用户各状态订单数量。
     *
     * <p>用户身份只取自请求头。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 状态计数统一响应。
     */
    @GetMapping("/status-counts")
    public Result<OrderStatusCountsVo> statusCounts(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.countStatusByUser(userId));
    }

    /**
     * 创建单 SKU 订单。
     *
     * <p>兼容已弃用入口，用户身份取自请求头，请求体提供 SKU、数量和收货信息。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param dto 已弃用的单 SKU 创建请求
     * @return 新订单 ID 统一响应。
     * @throws BizException SKU 快照缺失、库存不足、ID 或支付流水创建失败。
     */
    @Deprecated
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotency
    public Result<Long> create(
            @RequestHeader("X-User-Id") Long userId, @Valid @RequestBody OrderCreateDto dto) {
        return Result.success(
                orderService.create(userId, dto.getSkuId(), dto.getQuantity(), dto.getReceiver()));
    }

    /**
     * 提交用户结算请求。
     *
     * <p>用户身份取自请求头，结算编排由 OrderCheckoutService 完成。
     *
     * @param userId 用户 ID，必须对应当前订单归属
     * @param dto 用户结算请求
     * @return 新订单 ID 统一响应。
     * @throws BizException 结算参数、快照、库存或订单创建失败。
     */
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
//    @Idempotency(business = "order:", type = "checkout:",id="#userId:", key = "#idempotencyKey")
    @Idempotency(key = "order:checkout:#userId:#idempotencyKey", replayType = OrderCheckoutReplayHandler.REPLAY_TYPE)
    public Result<Long> checkout(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OrderCheckoutDto dto) {

        // 去除真正格式化后的 idempotencyKey
        idempotencyKey = (String) RequestContextHolder.currentRequestAttributes()
                .getAttribute("idempotencyKey", RequestAttributes.SCOPE_REQUEST);
        return Result.success(orderCheckoutService.checkout(userId, dto, idempotencyKey));
    }

    /**
     * 将结算参数校验失败转换为 HTTP 400.
     *
     * @param exception 参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> checkoutValidation(
            MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(Result.fail(CommonCode.PARAM_INVALID, "结算参数校验失败"));
    }

    /**
     * 将无法解析的结算请求转换为 HTTP 400.
     *
     * @param exception 请求体解析异常
     * @return 统一响应结果
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> checkoutUnreadable(
            HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(Result.fail(CommonCode.PARAM_INVALID, "结算来源或请求体格式错误"));
    }

    /**
     * 按业务错误码映射结算异常的 HTTP 状态。
     *
     * <p>PARAM_INVALID 返回 400，其他 BizException 返回 HTTP 200 的失败 Result。
     *
     * @param exception 结算链路业务异常
     * @return 对应 HTTP 状态及统一失败响应。
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> checkoutBiz(BizException exception) {
        if (CommonCode.PARAM_INVALID.equals(exception.getCode())) {
            return ResponseEntity.badRequest()
                    .body(Result.fail(exception.getCode(), exception.getMessage()));
        }
        return ResponseEntity.ok(Result.fail(exception.getCode(), exception.getMessage()));
    }

    /**
     * 查询当前用户订单状态。
     *
     * <p>服务层校验订单归属并返回状态码。
     *
     * @param id 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 状态码统一响应。
     */
    @GetMapping("/{id}")
    public Result<Integer> status(
            @PathVariable("id") Long id, @RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.getStatus(id, userId));
    }

    /**
     * 查询当前用户订单详情。
     *
     * <p>服务层校验归属并补充订单明细。
     *
     * @param id 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 订单详情统一响应。
     */
    @GetMapping("/{id}/detail")
    public Result<OrderVo> detail(
            @PathVariable("id") Long id, @RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.detailVo(id, userId));
    }

    /**
     * 取消当前用户订单。
     *
     * <p>订单 ID 来自路径，用户 ID 来自请求头。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 空数据的成功响应。
     * @throws BizException 订单不存在、归属不匹配或状态不允许取消。
     */
    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancel(@PathVariable Long orderId, @RequestHeader("X-User-Id") Long userId) {
        orderService.cancel(orderId, userId);
        return Result.success(null);
    }

    /**
     * 确认当前用户订单收货。
     *
     * <p>服务层校验订单归属和已发货状态。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 空数据的成功响应。
     * @throws BizException 订单不存在、归属不匹配或状态不允许确认。
     */
    @PostMapping("/{orderId}/confirm")
    public Result<Void> confirm(@PathVariable Long orderId, @RequestHeader("X-User-Id") Long userId) {
        orderService.confirm(orderId, userId);
        return Result.success(null);
    }

    /**
     * 提交当前用户退款申请。
     *
     * <p>服务层登记退款意向，不直接调用支付退款。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 空数据的成功响应。
     * @throws BizException 订单不存在、归属不匹配或状态不允许申请退款。
     */
    @PostMapping("/{orderId}/refund")
    @Idempotency
    public Result<Void> refund(@PathVariable Long orderId, @RequestHeader("X-User-Id") Long userId) {
        orderService.refund(orderId, userId);
        return Result.success(null);
    }

    /**
     * 删除当前用户终态订单。
     *
     * <p>仅允许删除已取消、已完成或已退款订单。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 空数据的成功响应。
     * @throws BizException 订单不存在、归属不匹配或状态不允许删除。
     */
    @DeleteMapping("/{orderId}")
    public Result<Void> delete(@PathVariable Long orderId, @RequestHeader("X-User-Id") Long userId) {
        orderService.delete(orderId, userId);
        return Result.success(null);
    }

    /**
     * 查询当前用户订单物流。
     *
     * <p>服务层校验归属并按时间升序返回节点。
     *
     * @param orderId 订单 ID
     * @param userId 用户 ID，必须对应当前订单归属
     * @return 物流节点统一响应。
     */
    @GetMapping("/{orderId}/logistics")
    public Result<List<OrderLogisticsVo>> logistics(
            @PathVariable Long orderId, @RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.queryLogistics(orderId, userId));
    }
}
