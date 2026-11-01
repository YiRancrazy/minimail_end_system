package com.yirancrazy.minimall.common.security;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 装配本地 AES-256 密钥提供者，并在应用启动时初始化全局加密上下文。
 * 外部可通过提供自定义 VaultKeyProvider Bean 替换默认密钥来源。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@AutoConfiguration
public class EncryptionAutoConfiguration {

    /**
     * 创建默认本地密钥提供者，读取 Base64 编码的 32 字节 AES 密钥。
     *
     * @param base64Key Base64 编码的 AES-256 密钥
     * @return 本地密钥提供者
     */
    @Bean
    @ConditionalOnMissingBean(VaultKeyProvider.class)
    public VaultKeyProvider vaultKeyProvider(
            @Value("${minimall.security.aes-key:${MINIMALL_SECURITY_AES_KEY}}") String base64Key) {
        return new LocalVaultKeyProvider(base64Key);
    }

    /**
     * 创建启动初始化器，在 Bean 属性设置完成后把密钥写入 EncryptionContext。
     *
     * @param keyProvider 提供 AES-256 密钥的组件
     * @return 加密上下文初始化器
     */
    @Bean
    public EncryptionInitializer encryptionInitializer(VaultKeyProvider keyProvider) {
        return new EncryptionInitializer(keyProvider);
    }

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: 在 Spring 启动阶段把 VaultKeyProvider 提供的密钥初始化到 EncryptionContext，
     * 确保后续 TypeHandler 调用前密钥已经可用。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     */
    static class EncryptionInitializer implements InitializingBean {

        private final VaultKeyProvider keyProvider;

        /**
         * 创建加密上下文初始化器。
         *
         * @param keyProvider AES-256 密钥提供者
         */
        EncryptionInitializer(VaultKeyProvider keyProvider) {
            this.keyProvider = keyProvider;
        }

        /** Bean 初始化完成后把密钥副本写入全局加密上下文。 */
        public void afterPropertiesSet() {
            EncryptionContext.initialize(keyProvider.getKey());
        }
    }
}
