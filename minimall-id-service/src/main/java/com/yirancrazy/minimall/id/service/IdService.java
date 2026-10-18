package com.yirancrazy.minimall.id.service;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface IdService {

    /**
     * 执行 nextId 方法。
     * @param bizTag bizTag 参数
     * @return 业务数值
     */
    long nextId(String bizTag);
}