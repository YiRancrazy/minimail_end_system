package com.yirancrazy.minimall.id.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.yirancrazy.minimall.id.service.IdService;
import com.yirancrazy.minimall.id.service.Snowflake;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Service
public final class IdServiceImpl implements IdService {

    private final Snowflake snowflake;


    /**
     * 构造 IdServiceImpl 实例。
     * @param datacenterId datacenterId 参数
     * @param workerId workerId 参数
     */
    public IdServiceImpl(
        @Value("${minimall.id.datacenter-id:${SNOWFLAKE_DATACENTER_ID:}}") String datacenterId,
        @Value("${minimall.id.worker-id:${SNOWFLAKE_WORKER_ID:}}") String workerId) {
        this.snowflake = new Snowflake(parseNodeId(datacenterId, "datacenter-id"),
            parseNodeId(workerId, "worker-id"));
    }


    /**
     * 执行 nextId 方法。
     * @param bizTag bizTag 参数
     * @return 业务数值
     */
    @Override
    public long nextId(String bizTag) {
        return snowflake.nextId();
    }


    /**
     * 转换或解析NodeID。
     * @param value 值
     * @param name 名称
     * @return 处理结果
     */
    private static long parseNodeId(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少配置项 minimall.id." + name);
        }
        try {
            return Long.parseLong(value.trim());
        }
        catch (NumberFormatException e) {
            throw new IllegalStateException("配置项 minimall.id." + name + " 非法：" + value, e);
        }
    }
}
