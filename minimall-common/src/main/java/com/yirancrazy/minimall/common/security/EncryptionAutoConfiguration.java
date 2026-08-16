package com.yirancrazy.minimall.common.security;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 加密自动装配：提供默认 VaultKeyProvider（prod 可用自定义实现替换），并随容器启动初始化
 *               EncryptionContext，使各服务（user/pay/merchant 等）字段级加密 typeHandler 生效。
 * @Version: 2.0
 * @DateTime: 2026/08/13
 */
@AutoConfiguration
public class EncryptionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(VaultKeyProvider.class)
    public VaultKeyProvider vaultKeyProvider(
            @Value("${minimall.security.aes-key:${MINIMALL_SECURITY_AES_KEY}}") String base64Key) {
        return new LocalVaultKeyProvider(base64Key);
    }

    @Bean
    public EncryptionInitializer encryptionInitializer(VaultKeyProvider keyProvider) {
        return new EncryptionInitializer(keyProvider);
    }

    /**
     * 初始化加密上下文，使静态持有的密钥在 MyBatis typeHandler 中可用。
     */
    static class EncryptionInitializer implements InitializingBean {

        private final VaultKeyProvider keyProvider;

        EncryptionInitializer(VaultKeyProvider keyProvider) {
            this.keyProvider = keyProvider;
        }

        @Override
        public void afterPropertiesSet() {
            EncryptionContext.initialize(keyProvider.getKey());
        }
    }
}
