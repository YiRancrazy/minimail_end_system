package com.yirancrazy.minimall.common.handle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;

/**
 * @Author: YiRanCrazy@gmail.com
 * @Description: 幂等键上下文
 * @Version: 1.0
 * @DateTime: 2026/11/1 19:34
 **/

@Data
@AllArgsConstructor
public final class IdempotencyContext {
    private final Method method;      // 目标方法，用于确认返回类型
    private final Object[] args;      // 实参，业务可按需取 userId 等
    private final String idempotencyKey;         // 格式化后的键，即落库的那个值
}