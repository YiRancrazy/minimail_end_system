package com.yirancrazy.minimall.common.support;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MDC 透传任务装饰器：异步线程池执行任务前复制提交线程的 MDC（含 traceId），
 *               执行后恢复执行线程原有上下文，避免异步日志丢失链路 ID。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 */
public class MdcTaskDecorator implements TaskDecorator {

    /**
     * 包装任务，在异步线程中先写入提交线程的 MDC 快照，任务结束后还原执行线程原上下文。
     * @param runnable 原始任务
     * @return 携带 MDC 上下文的新任务
     */
    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> submitContext = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                if (submitContext == null) {
                    MDC.clear();
                }
                else {
                    MDC.setContextMap(submitContext);
                }
                runnable.run();
            }
            finally {
                if (previous == null) {
                    MDC.clear();
                }
                else {
                    MDC.setContextMap(previous);
                }
            }
        };
    }
}
