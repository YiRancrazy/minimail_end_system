package com.yirancrazy.minimall.id.service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ID 领域服务接口，定义基于业务标签获取全局唯一 ID 的契约。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public interface IdService {
    long nextId(String bizTag);
}