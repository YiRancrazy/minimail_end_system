package com.yirancrazy.minimall.common.security;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 加密上下文，以静态方式持有AES-256密钥，供MyBatis类型处理器在非Spring管理的实例中访问。由EncryptionAutoConfiguration在启动时初始化。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
public final class EncryptionContext {

    private static volatile byte[] key;

    private EncryptionContext() {
    }

    /**
     * 初始化加密密钥，由EncryptionAutoConfiguration在启动时调用。
     *
     * @param aesKey 32字节AES-256密钥
     */
    static void initialize(byte[] aesKey) {
        key = aesKey.clone();
    }

    /**
     * 获取当前加密密钥。
     *
     * @return 密钥字节数组的副本
     * @throws IllegalStateException 如果密钥未初始化
     */
    public static byte[] getKey() {
        if (key == null) {
            throw new IllegalStateException("EncryptionContext not initialized");
        }
        return key.clone();
    }
}
