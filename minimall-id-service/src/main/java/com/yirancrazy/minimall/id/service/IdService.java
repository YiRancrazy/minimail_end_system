package com.yirancrazy.minimall.id.service;

/**
* ID 领域服务接口，定义基于业务标签获取全局唯一 ID 的契约。
 */
public interface IdService {
    /**
     * 获取下一个全局唯一ID。
     * @param bizTag 业务标签
     * @return 全局唯一ID
     */
    long nextId(String bizTag);
}