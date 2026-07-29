package com.yirancrazy.minimall.common.event;

/**
 * Cross-process event publication abstraction. Iterate-6 replaces the
 * in-process LocalEventBus with a RocketMQ-backed implementation behind
 * this interface. Local impl is kept as the default for dev/test profiles.
 */
public interface EventBus {
    void publish(Object event);
}
