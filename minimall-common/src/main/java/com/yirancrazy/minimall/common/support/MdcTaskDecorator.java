package com.yirancrazy.minimall.common.support;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MdcTaskDecorator 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class MdcTaskDecorator implements TaskDecorator {


    /**
     * 执行 decorate 方法。
     * @param runnable runnable 参数
     * @return 处理结果
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
