package com.yirancrazy.minimall.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutCleanupDto;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDto;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.constant.CartCleanupStatusEnum;
import com.yirancrazy.minimall.order.entity.CartCleanupTaskPo;
import com.yirancrazy.minimall.order.gateway.OrderRemoteGateway;
import com.yirancrazy.minimall.order.manager.CartCleanupTaskManager;
import com.yirancrazy.minimall.order.service.CartCleanupService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 实现购物车清理任务的领取、JSON 校验、调用远程清理和失败分级处理。
 *   业务边界：可重试错误保持 PENDING 并指数退避，参数或 SKU 不匹配等错误直接标记 FAILED；单批最多 100 条。
 *   使用场景：结算成功后立即调用，或由定时扫描继续执行。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartCleanupServiceImpl implements CartCleanupService {

  private static final int SCAN_LIMIT = 100;
  private static final int MAX_BACKOFF_MINUTES = 30;
  private static final int CLAIM_SECONDS = 30;
  private static final int MAX_ERROR_LENGTH = 500;
  private static final Set<String> NON_RETRYABLE_CODES = Set.of("14004", CommonCode.PARAM_INVALID);

  private final CartCleanupTaskManager taskManager;
  private final OrderRemoteGateway orderRemoteGateway;
  private final ObjectMapper objectMapper;

  /**
   * 尝试执行一条购物车清理任务。
   *
   * <p>已成功任务直接返回 true；待处理任务需先条件抢占，再解析 JSON、校验条目并调用购物车服务。可重试错误保持 PENDING，参数或 SKU 不匹配等错误直接失败。
   *
   * @param taskId 购物车清理任务 ID
   * @return 任务已成功或本次清理成功时返回 true。
   * @throws BizException 任务参数或远端错误会转换为任务状态；方法本身不向调度器抛出已捕获的运行时异常。
   */
  @Override
  public boolean tryCleanup(Long taskId) {
    CartCleanupTaskPo task = taskManager.getById(taskId);
    if (task == null) {
      return false;
    }
    if (task.getStatus() == CartCleanupStatusEnum.SUCCESS.intCode()) {
      return true;
    }
    if (task.getStatus() == CartCleanupStatusEnum.FAILED.intCode()) {
      return false;
    }
    if (!claimTask(task)) {
      return false;
    }
    List<CartCheckoutItemDto> items;
    try {
      items =
          objectMapper.readValue(
              task.getItemsJson(), new TypeReference<List<CartCheckoutItemDto>>() {});
    } catch (JsonProcessingException exception) {
      markFailed(task, "购物车清理任务数据损坏");
      return false;
    }
    if (!isValidCleanupItems(items)) {
      markFailed(task, "购物车清理任务字段非法");
      return false;
    }
    try {
      Result<Void> result =
          orderRemoteGateway.cleanupCheckoutCart(
              new CartCheckoutCleanupDto(task.getUserId(), items));
      if (result != null && CommonCode.SUCCESS.equals(result.getCode())) {
        markSuccess(task);
        return true;
      }
      String code = result == null ? null : result.getCode();
      String message = result == null ? "购物车清理结果为空" : result.getMessage();
      if (code != null && NON_RETRYABLE_CODES.contains(code)) {
        markFailed(task, message);
      } else {
        markRetryableFailure(task, message);
      }
      return false;
    } catch (RuntimeException exception) {
      markRetryableFailure(task, exception.getMessage());
      return false;
    }
  }

  /**
   * 扫描并执行已到期的购物车清理任务。
   *
   * <p>单批最多 100 条，按 nextRetryAt 和 ID 排序；单条运行时异常不会阻断后续任务。
   *
   * @return 本轮成功清理的任务数量。
   */
  @Override
  public int scanPending() {
    List<CartCleanupTaskPo> tasks =
        taskManager.list(
            Wrappers.lambdaQuery(CartCleanupTaskPo.class)
                .eq(CartCleanupTaskPo::getStatus, CartCleanupStatusEnum.PENDING.intCode())
                .le(CartCleanupTaskPo::getNextRetryAt, LocalDateTime.now())
                .orderByAsc(CartCleanupTaskPo::getNextRetryAt)
                .orderByAsc(CartCleanupTaskPo::getId)
                .last("LIMIT " + SCAN_LIMIT));
    int cleaned = 0;
    for (CartCleanupTaskPo task : tasks) {
      try {
        if (tryCleanup(task.getId())) {
          cleaned++;
        }
      } catch (RuntimeException exception) {
        log.error("购物车清理任务执行异常，taskId={}", task.getId(), exception);
      }
    }
    return cleaned;
  }

  /**
   * 抢占一条到期的购物车清理任务。
   *
   * <p>通过 PENDING 与 nextRetryAt 条件更新把任务延后 30 秒，避免并发重复执行。
   *
   * @param task 购物车清理任务记录
   * @return 更新成功时返回 true。
   */
  private boolean claimTask(CartCleanupTaskPo task) {
    LocalDateTime now = LocalDateTime.now();
    return taskManager.update(
        Wrappers.lambdaUpdate(CartCleanupTaskPo.class)
            .eq(CartCleanupTaskPo::getId, task.getId())
            .eq(CartCleanupTaskPo::getStatus, CartCleanupStatusEnum.PENDING.intCode())
            .le(CartCleanupTaskPo::getNextRetryAt, now)
            .set(CartCleanupTaskPo::getNextRetryAt, now.plusSeconds(CLAIM_SECONDS)));
  }

  /**
   * 把购物车清理任务标记为成功。
   *
   * <p>写入 SUCCESS 并清空最近错误，不删除任务记录。
   *
   * @param task 购物车清理任务记录
   */
  private void markSuccess(CartCleanupTaskPo task) {
    task.setStatus(CartCleanupStatusEnum.SUCCESS.intCode());
    task.setLastError(null);
    taskManager.updateById(task);
  }

  /**
   * 校验购物车清理条目结构和唯一性。
   *
   * <p>要求列表非空，购物车项 ID、SKU ID、正整数数量齐全，且 cartItemId 不重复。
   *
   * @param items 订单 SKU 与数量明细
   * @return 结构合法时返回 true。
   */
  private boolean isValidCleanupItems(List<CartCheckoutItemDto> items) {
    if (items == null || items.isEmpty()) {
      return false;
    }
    Set<Long> ids = new HashSet<>(items.size() * 2);
    for (CartCheckoutItemDto item : items) {
      if (item == null
          || item.getCartItemId() == null
          || item.getSkuId() == null
          || item.getQuantity() == null
          || item.getQuantity() <= 0
          || !ids.add(item.getCartItemId())) {
        return false;
      }
    }
    return true;
  }

  /**
   * 记录可重试的购物车清理失败。
   *
   * <p>任务保持 PENDING，重试次数加一，并按上限 30 分钟的指数退避计算 nextRetryAt。
   *
   * @param task 购物车清理任务记录
   * @param error 失败原因，持久化前按最大长度截断
   */
  private void markRetryableFailure(CartCleanupTaskPo task, String error) {
    int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
    task.setStatus(CartCleanupStatusEnum.PENDING.intCode());
    task.setRetryCount(retryCount + 1);
    task.setNextRetryAt(
        LocalDateTime.now()
            .plusMinutes(Math.min(MAX_BACKOFF_MINUTES, 1L << Math.min(retryCount, 5))));
    task.setLastError(truncate(error));
    taskManager.updateById(task);
  }

  /**
   * 把不可重试的购物车清理任务标记为失败。
   *
   * <p>写入 FAILED、截断错误信息并记录错误日志，不再参与扫描。
   *
   * @param task 购物车清理任务记录
   * @param error 失败原因，持久化前按最大长度截断
   */
  private void markFailed(CartCleanupTaskPo task, String error) {
    task.setStatus(CartCleanupStatusEnum.FAILED.intCode());
    task.setLastError(truncate(error));
    taskManager.updateById(task);
    log.error("购物车清理永久失败，taskId={}，error={}", task.getId(), error);
  }

  /**
   * 将错误信息截断到数据库允许长度。
   *
   * <p>null 保持 null，超长文本只保留前 500 个字符。
   *
   * @param value 待序列化的快照对象
   * @return 截断后的文本或 null。
   */
  private String truncate(String value) {
    if (value == null) {
      return null;
    }
    return value.length() <= MAX_ERROR_LENGTH ? value : value.substring(0, MAX_ERROR_LENGTH);
  }
}
