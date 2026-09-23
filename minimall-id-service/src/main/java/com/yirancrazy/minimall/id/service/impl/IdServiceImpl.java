package com.yirancrazy.minimall.id.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.yirancrazy.minimall.id.service.IdService;
import com.yirancrazy.minimall.id.service.Snowflake;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ID生成领域服务实现，实现Id相关业务逻辑
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Service
public final class IdServiceImpl implements IdService {

    private final Snowflake snowflake;

    /**
     * 构造雪花 ID 服务，节点编号必须由环境显式提供。
     * @param datacenterId 数据中心ID，范围 0~31
     * @param workerId 工作机器ID，范围 0~31
     */
    public IdServiceImpl(
        @Value("${minimall.id.datacenter-id:${SNOWFLAKE_DATACENTER_ID:}}") String datacenterId,
        @Value("${minimall.id.worker-id:${SNOWFLAKE_WORKER_ID:}}") String workerId) {
        this.snowflake = new Snowflake(parseNodeId(datacenterId, "datacenter-id"),
            parseNodeId(workerId, "worker-id"));
    }

    /**
     * 根据业务标签获取下一个全局唯一 ID，当前实现直接委托给内部 Snowflake 实例生成。
     *
     * @param bizTag 业务标签，用于区分不同业务域（当前实现仅透传，不参与 ID 生成）
     * @return 新生成的分布式唯一 ID
     */
    @Override
    public long nextId(String bizTag) {
        return snowflake.nextId();
    }

    /**
     * 解析并校验雪花节点编号，缺失或非数字时阻止服务启动。
     * @param value 配置值
     * @param name 配置项名称
     * @return 节点编号
     */
    private static long parseNodeId(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("missing minimall.id." + name);
        }
        try {
            return Long.parseLong(value.trim());
        }
        catch (NumberFormatException e) {
            throw new IllegalStateException("invalid minimall.id." + name + ": " + value, e);
        }
    }
}
