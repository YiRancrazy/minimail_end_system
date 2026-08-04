package com.yirancrazy.minimall.common.security;

import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 加密自动配置类，在Spring容器启动时从VaultKeyProvider获取密钥并初始化EncryptionContext。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@Configuration
public class EncryptionAutoConfiguration {

    private final VaultKeyProvider keyProvider;

    /**
     * 构造函数，注入Vault密钥提供者。
     *
     * @param keyProvider Vault密钥提供者
     */
    public EncryptionAutoConfiguration(VaultKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    /**
     * 在Bean初始化后，将密钥注入EncryptionContext静态持有者。
     */
    @PostConstruct
    public void initializeEncryption() {
        EncryptionContext.initialize(keyProvider.getKey());
    }
}
