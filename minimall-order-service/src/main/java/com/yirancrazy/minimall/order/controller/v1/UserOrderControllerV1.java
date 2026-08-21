package com.yirancrazy.minimall.order.controller.v1;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.annotation.Idempotent;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.dto.OrderCheckoutDTO;
import com.yirancrazy.minimall.order.dto.OrderCreateDTO;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderLogisticsVO;
import com.yirancrazy.minimall.order.vo.OrderStatusCountsVO;
import com.yirancrazy.minimall.order.vo.OrderVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户端订单控制器，提供用户视角的订单 RESTful API
 * @Version: 1.0
 * @DateTime: 2026/08/04
 */
@RestController
@RequestMapping("/api/v1/user/orders")
public class UserOrderControllerV1 {

    private final OrderService orderService;

    public UserOrderControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 游标分页查询订单列表，支持按用户/商家/状态过滤。
     * @param dto 游标分页查询入参
     * @return 订单游标分页结果
     */
    @GetMapping
    public Result<CursorPageVO<OrderVO>> page(@Valid OrderPageDTO dto) {
        return Result.success(orderService.page(dto).map(OrderVO::from));
    }

    /**
     * 统计当前用户各状态订单数量，供"我的"页面订单快捷入口角标展示。
     * @param userId 用户ID，来自网关 X-User-Id 头
     * @return 各状态订单数量VO
     */
    @GetMapping("/status-counts")
    public Result<OrderStatusCountsVO> statusCounts(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.countStatusByUser(userId));
    }

    /**
     * 创建订单并完成库存锁定及支付流水初始化。
     *
     * @param userId 用户ID，来自网关 X-User-Id 头
     * @param dto 订单创建请求参数
     * @return 新创建的订单标识
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public Result<Long> create(@RequestHeader("X-User-Id") Long userId,
                               @Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.create(userId, dto.getSkuId(), dto.getQuantity()));
    }

    /**
     * 多SKU结算下单，支持购物车批量结算。
     * @param userId 用户ID，来自网关 X-User-Id 头
     * @param dto 结算请求
     * @return 新创建的订单ID
     */
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public Result<Long> checkout(@RequestHeader("X-User-Id") Long userId,
                                 @Valid @RequestBody OrderCheckoutDTO dto) {
        return Result.success(orderService.checkout(userId, dto.getItems()));
    }

    /**
     * 推进指定待支付订单的支付流程并返回处理结果。
     *
     * @param id 订单标识
     * @param userId 用户ID，来自网关 X-User-Id 头
     * @return 支付成功返回 true，订单状态不允许支付时返回 false
     */
    @PostMapping("/{id}/pay")
    @Idempotent
    public Result<Void> pay(@PathVariable("id") Long id,
                            @RequestHeader("X-User-Id") Long userId) {
        orderService.pay(id, userId);
        return Result.success(null);
    }

    /**
     * 查询订单状态。
     * @param id 订单ID
     * @param userId 用户ID，来自网关 X-User-Id 头
     * @return 订单状态
     */
    @GetMapping("/{id}")
    public Result<Integer> status(@PathVariable("id") Long id,
                                  @RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.getStatus(id, userId));
    }

    /**
     * 查询订单详情。
     * @param id 订单ID
     * @param userId 用户ID，来自网关 X-User-Id 头
     * @return 订单实体
     */
    @GetMapping("/{id}/detail")
    public Result<OrderVO> detail(@PathVariable("id") Long id,
                                  @RequestHeader("X-User-Id") Long userId) {
        OrderPO po = orderService.getDetail(id, userId);
        OrderVO vo = OrderVO.from(po);
        vo.setItems(orderService.listItemVO(id));
        return Result.success(vo);
    }

    /**
     * 取消订单。
     * @param orderId 订单ID
     * @param userId  当前用户ID
     */
    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancel(@PathVariable Long orderId,
                               @RequestHeader("X-User-Id") Long userId) {
        orderService.cancel(orderId, userId);
        return Result.success(null);
    }

    /**
     * 确认收货。
     * @param orderId 订单ID
     * @param userId  当前用户ID
     */
    @PostMapping("/{orderId}/confirm")
    public Result<Void> confirm(@PathVariable Long orderId,
                                @RequestHeader("X-User-Id") Long userId) {
        orderService.confirm(orderId, userId);
        return Result.success(null);
    }

    /**
     * 申请退款：仅将订单置为退款中并记录原状态，真实退款由商家审核通过后触发。
     * @param orderId 订单ID
     * @param userId 当前用户ID，来自网关 X-User-Id 头
     */
    @PostMapping("/{orderId}/refund")
    @Idempotent
    public Result<Void> refund(@PathVariable Long orderId,
                               @RequestHeader("X-User-Id") Long userId) {
        orderService.refund(orderId, userId);
        return Result.success(null);
    }

    /**
     * 用户删除订单，仅允许终态订单软删除。
     * @param orderId 订单ID
     * @param userId 用户ID
     */
    @DeleteMapping("/{orderId}")
    public Result<Void> delete(@PathVariable Long orderId,
                               @RequestHeader("X-User-Id") Long userId) {
        orderService.delete(orderId, userId);
        return Result.success(null);
    }

    /**
     * 查询订单物流轨迹，按创建时间正序返回。
     * @param orderId 订单ID
     * @param userId 用户ID，来自网关 X-User-Id 头
     * @return 物流节点列表
     */
    @GetMapping("/{orderId}/logistics")
    public Result<List<OrderLogisticsVO>> logistics(@PathVariable Long orderId,
                                                    @RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.queryLogistics(orderId, userId));
    }
}
