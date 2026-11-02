package com.yirancrazy.minimall.common.registry;

import com.yirancrazy.minimall.common.handle.IdempotencyReplayHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: YiRanCrazy@gmail.com
 * @Description: 幂等重放处理器注册表
 * @Version: 1.0
 * @DateTime: 2026/11/2 8:18
 **/

public class IdempotencyReplayHandlerRegistry {

    private final Map<String, IdempotencyReplayHandler> handlers;

    /**
     * 构造函数
     * @param handlers 幂等重放处理器列表
     */
    public IdempotencyReplayHandlerRegistry(List<IdempotencyReplayHandler> handlers) {
        Map<String,IdempotencyReplayHandler> map = new HashMap<>();
        for (IdempotencyReplayHandler handler : handlers){
            String type = handler.replayType();

            if (type == null || type.isBlank()) {
                throw new IllegalStateException("幂等重放处理器缺少业务类型");
            }
            if (map.putIfAbsent(type, handler) != null) {
                throw new IllegalStateException("重复的幂等重放业务类型: " + type);
            }
        }
        this.handlers = Map.copyOf(map);
    }

    /**
     * 根据业务类型查找幂等重放处理器
     * @param replayType 业务类型
     * @return 幂等重放处理器
     */
    public IdempotencyReplayHandler find(String replayType) {
        return handlers.get(replayType);
    }

}
