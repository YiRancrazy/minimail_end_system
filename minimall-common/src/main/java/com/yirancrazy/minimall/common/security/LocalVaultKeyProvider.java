package com.yirancrazy.minimall.common.security;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 从 Spring 配置或环境变量读取 Base64 编码的 AES-256 密钥，并在构造时校验长度。
 * 该实现适合无外部密钥管理系统的本地或测试环境。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class LocalVaultKeyProvider implements VaultKeyProvider {

    private final byte[] key;

    /**
     * 解码并校验 Base64 密钥长度。
     *
     * @param base64Key Base64 编码的 32 字节 AES-256 密钥
     * @throws IllegalArgumentException Base64 非法或解码后不是 32 字节时抛出
     */
    public LocalVaultKeyProvider(
            @Value("${minimall.security.aes-key:${MINIMALL_SECURITY_AES_KEY}}") String base64Key) {
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        if (decoded.length != 32) {
            throw new IllegalArgumentException("AES-256 密钥必须为 32 字节，实际为 " + decoded.length);
        }
        this.key = decoded;
    }

    /**
     * 返回密钥的防御性副本。
     *
     * @return 32 字节 AES-256 密钥副本
     */
    @Override
    public byte[] getKey() {
        return key.clone();
    }
}
