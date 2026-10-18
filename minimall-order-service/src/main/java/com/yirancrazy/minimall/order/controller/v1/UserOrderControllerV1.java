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
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderLogisticsVO;
import com.yirancrazy.minimall.order.vo.OrderStatusCountsVO;
import com.yirancrazy.minimall.order.vo.OrderVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserOrderControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/user/orders")
public class UserOrderControllerV1 {

    private final OrderService orderService;

    /**
     * 构造 UserOrderControllerV1 实例。
     * @param orderService orderService 参数
     */
    public UserOrderControllerV1(OrderService orderService) {
        this.orderService = orderService;
    }


    /**
     * 分页查询数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<CursorPageVO<OrderVO>> page(@RequestHeader("X-User-Id") Long userId,
                                              @Valid OrderPageDTO dto) {
        dto.setUserId(userId);
        return Result.success(orderService.pageVO(dto));
    }


    /**
     * 执行 statusCounts 方法。
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping("/status-counts")
    public Result<OrderStatusCountsVO> statusCounts(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.countStatusByUser(userId));
    }


    /**
     * 创建业务数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public Result<Long> create(@RequestHeader("X-User-Id") Long userId,
                               @Valid @RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.create(userId, dto.getSkuId(), dto.getQuantity(), dto.getReceiver()));
    }


    /**
     * 校验业务条件。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public Result<Long> checkout(@RequestHeader("X-User-Id") Long userId,
                                 @Valid @RequestBody OrderCheckoutDTO dto) {
        return Result.success(orderService.checkout(userId, dto.getItems(), dto.getReceiver()));
    }


    /**
     * 执行 status 方法。
     * @param id id 参数
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping("/{id}")
    public Result<Integer> status(@PathVariable("id") Long id,
                                  @RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.getStatus(id, userId));
    }


    /**
     * 执行 detail 方法。
     * @param id id 参数
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping("/{id}/detail")
    public Result<OrderVO> detail(@PathVariable("id") Long id,
                                  @RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.detailVO(id, userId));
    }


    /**
     * 执行 cancel 方法。
     * @param orderId orderId 参数
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancel(@PathVariable Long orderId,
                               @RequestHeader("X-User-Id") Long userId) {
        orderService.cancel(orderId, userId);
        return Result.success(null);
    }


    /**
     * 执行 confirm 方法。
     * @param orderId orderId 参数
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @PostMapping("/{orderId}/confirm")
    public Result<Void> confirm(@PathVariable Long orderId,
                                @RequestHeader("X-User-Id") Long userId) {
        orderService.confirm(orderId, userId);
        return Result.success(null);
    }


    /**
     * 执行 refund 方法。
     * @param orderId orderId 参数
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @PostMapping("/{orderId}/refund")
    @Idempotent
    public Result<Void> refund(@PathVariable Long orderId,
                               @RequestHeader("X-User-Id") Long userId) {
        orderService.refund(orderId, userId);
        return Result.success(null);
    }


    /**
     * 删除业务数据。
     * @param orderId orderId 参数
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @DeleteMapping("/{orderId}")
    public Result<Void> delete(@PathVariable Long orderId,
                               @RequestHeader("X-User-Id") Long userId) {
        orderService.delete(orderId, userId);
        return Result.success(null);
    }


    /**
     * 执行 logistics 方法。
     * @param orderId orderId 参数
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping("/{orderId}/logistics")
    public Result<List<OrderLogisticsVO>> logistics(@PathVariable Long orderId,
                                                    @RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.queryLogistics(orderId, userId));
    }
}
