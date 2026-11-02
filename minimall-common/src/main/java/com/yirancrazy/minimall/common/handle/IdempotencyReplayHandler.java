package com.yirancrazy.minimall.common.handle;

/**
 * @Author: YiRanCrazy@gmail.com
 * @Description: 幂等性重试处理器，用于处理幂等键已被占用的情况,各业务自行实现。
 * @Version: 1.0
 * @DateTime: 2026/11/1 19:33
 **/
public interface IdempotencyReplayHandler {

    /**
     * 获取幂等键的重试类型。
     *
     * @return 幂等键的重试类型
     */
    String replayType();

    /**
     * 幂等键已被占用时回调。
     * @return 与目标方法返回类型一致的结果；返回 null 表示无法回放（处理中或不存在）
     */
    Object onDuplicate(IdempotencyContext context) throws Throwable;
}
