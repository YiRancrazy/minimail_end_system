package com.yirancrazy.minimall.id.service;

/**
* ID 领域服务接口，定义基于业务标签获取全局唯一 ID 的契约。
 */
public interface IdService {
    long nextId(String bizTag);
}