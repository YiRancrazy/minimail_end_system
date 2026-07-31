package com.yirancrazy.minimall.common.event;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 事件总线组件，提供事件发布订阅能力
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface EventBus {
    void publish(Object event);
}
