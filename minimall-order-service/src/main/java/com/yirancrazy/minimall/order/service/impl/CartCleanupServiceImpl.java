package com.yirancrazy.minimall.order.service.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutCleanupDTO;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDTO;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.constant.CartCleanupStatusEnum;
import com.yirancrazy.minimall.order.entity.CartCleanupTaskPO;
import com.yirancrazy.minimall.order.gateway.OrderRemoteGateway;
import com.yirancrazy.minimall.order.manager.CartCleanupTaskManager;
import com.yirancrazy.minimall.order.service.CartCleanupService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车结算清理任务服务实现。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class CartCleanupServiceImpl implements CartCleanupService {

    private static final int SCAN_LIMIT = 100;
    private static final int MAX_BACKOFF_MINUTES = 30;
    private static final int CLAIM_SECONDS = 30;
    private static final int MAX_ERROR_LENGTH = 500;
    private static final Set<String> NON_RETRYABLE_CODES =
        Set.of("14004", CommonCode.PARAM_INVALID);

    private final CartCleanupTaskManager taskManager;
    private final OrderRemoteGateway orderRemoteGateway;
    private final ObjectMapper objectMapper;

    /**
     * 尝试执行单个清理任务。
     * @param taskId 任务 ID
     * @return 是否清理成功
     */
    @Override
    public boolean tryCleanup(Long taskId) {
        CartCleanupTaskPO task = taskManager.getById(taskId);
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
        List<CartCheckoutItemDTO> items;
        try {
            items = objectMapper.readValue(task.getItemsJson(),
                new TypeReference<List<CartCheckoutItemDTO>>() { });
        }
        catch (JsonProcessingException exception) {
            markFailed(task, "购物车清理任务数据损坏");
            return false;
        }
        if (!isValidCleanupItems(items)) {
            markFailed(task, "购物车清理任务字段非法");
            return false;
        }
        try {
            Result<Void> result = orderRemoteGateway.cleanupCheckoutCart(
                new CartCheckoutCleanupDTO(task.getUserId(), items));
            if (result != null && CommonCode.SUCCESS.equals(result.getCode())) {
                markSuccess(task);
                return true;
            }
            String code = result == null ? null : result.getCode();
            String message = result == null ? "购物车清理结果为空" : result.getMessage();
            if (code != null && NON_RETRYABLE_CODES.contains(code)) {
                markFailed(task, message);
            }
            else {
                markRetryableFailure(task, message);
            }
            return false;
        }
        catch (RuntimeException exception) {
            markRetryableFailure(task, exception.getMessage());
            return false;
        }
    }

    /**
     * 扫描并执行到期的清理任务。
     * @return 成功清理数量
     */
    @Override
    public int scanPending() {
        List<CartCleanupTaskPO> tasks = taskManager.list(
            Wrappers.lambdaQuery(CartCleanupTaskPO.class)
                .eq(CartCleanupTaskPO::getStatus, CartCleanupStatusEnum.PENDING.intCode())
                .le(CartCleanupTaskPO::getNextRetryAt, LocalDateTime.now())
                .orderByAsc(CartCleanupTaskPO::getNextRetryAt)
                .orderByAsc(CartCleanupTaskPO::getId)
                .last("LIMIT " + SCAN_LIMIT));
        int cleaned = 0;
        for (CartCleanupTaskPO task : tasks) {
            try {
                if (tryCleanup(task.getId())) {
                    cleaned++;
                }
            }
            catch (RuntimeException exception) {
                log.error("购物车清理任务执行异常，taskId={}", task.getId(), exception);
            }
        }
        return cleaned;
    }

    private boolean claimTask(CartCleanupTaskPO task) {
        LocalDateTime now = LocalDateTime.now();
        return taskManager.update(Wrappers.lambdaUpdate(CartCleanupTaskPO.class)
            .eq(CartCleanupTaskPO::getId, task.getId())
            .eq(CartCleanupTaskPO::getStatus, CartCleanupStatusEnum.PENDING.intCode())
            .le(CartCleanupTaskPO::getNextRetryAt, now)
            .set(CartCleanupTaskPO::getNextRetryAt, now.plusSeconds(CLAIM_SECONDS)));
    }

    private void markSuccess(CartCleanupTaskPO task) {
        task.setStatus(CartCleanupStatusEnum.SUCCESS.intCode());
        task.setLastError(null);
        taskManager.updateById(task);
    }

    private boolean isValidCleanupItems(List<CartCheckoutItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }
        Set<Long> ids = new HashSet<>(items.size() * 2);
        for (CartCheckoutItemDTO item : items) {
            if (item == null || item.getCartItemId() == null
                || item.getSkuId() == null || item.getQuantity() == null
                || item.getQuantity() <= 0 || !ids.add(item.getCartItemId())) {
                return false;
            }
        }
        return true;
    }

    private void markRetryableFailure(CartCleanupTaskPO task, String error) {
        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        task.setStatus(CartCleanupStatusEnum.PENDING.intCode());
        task.setRetryCount(retryCount + 1);
        task.setNextRetryAt(LocalDateTime.now().plusMinutes(
            Math.min(MAX_BACKOFF_MINUTES, 1L << Math.min(retryCount, 5))));
        task.setLastError(truncate(error));
        taskManager.updateById(task);
    }

    private void markFailed(CartCleanupTaskPO task, String error) {
        task.setStatus(CartCleanupStatusEnum.FAILED.intCode());
        task.setLastError(truncate(error));
        taskManager.updateById(task);
        log.error("购物车清理永久失败，taskId={}，error={}", task.getId(), error);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_ERROR_LENGTH
            ? value
            : value.substring(0, MAX_ERROR_LENGTH);
    }
}
