package com.yirancrazy.minimall.id.service.impl;

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
public class IdServiceImpl implements IdService {

    private final Snowflake snowflake = new Snowflake(1L, 1L);

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
}