package com.yirancrazy.minimall.common.security;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: EncryptionContext 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public final class EncryptionContext {

    private static volatile byte[] key;

    /**
     * 构造 EncryptionContext 实例。
     */
    private EncryptionContext() {
    }


    /**
     * 初始化ialize。
     * @param aesKey aesKey
     */
    static void initialize(byte[] aesKey) {
        key = aesKey.clone();
    }


    /**
     * 获取相关数据。
     * @return 结果列表
     */
    public static byte[] getKey() {
        if (key == null) {
            throw new IllegalStateException("EncryptionContext 未初始化");
        }
        return key.clone();
    }
}
