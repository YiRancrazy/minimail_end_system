package com.yirancrazy.minimall.common.security;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: VaultKeyProvider 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface VaultKeyProvider {


    /**
     * 获取相关数据。
     * @return 结果列表
     */
    byte[] getKey();
}
