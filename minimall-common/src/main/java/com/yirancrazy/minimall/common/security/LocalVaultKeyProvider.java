package com.yirancrazy.minimall.common.security;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 本地Vault密钥提供者，从配置读取Base64编码的AES-256密钥。仅用于dev/test环境，生产环境应使用真正的Vault实现。
 * @Version: 1.1
 * @DateTime: 2026/08/13
 */
public class LocalVaultKeyProvider implements VaultKeyProvider {

    private final byte[] key;

    /**
     * 构造函数，从配置读取Base64编码的密钥并解码。
     *
     * @param base64Key Base64编码的32字节AES-256密钥
     */
    public LocalVaultKeyProvider(
        @Value("${minimall.security.aes-key:MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=}") String base64Key) {
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        if (decoded.length != 32) {
            throw new IllegalArgumentException("AES-256 key must be 32 bytes, got " + decoded.length);
        }
        this.key = decoded;
    }

    @Override
    public byte[] getKey() {
        return key.clone();
    }
}
