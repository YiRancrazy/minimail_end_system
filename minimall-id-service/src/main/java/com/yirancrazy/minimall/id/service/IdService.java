package com.yirancrazy.minimall.id.service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ID生成领域服务接口，定义Id相关业务契约
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface IdService {
    /**
     * 获取下一个全局唯一ID。
     * @param bizTag 业务标签
     * @return 全局唯一ID
     */
    long nextId(String bizTag);
}