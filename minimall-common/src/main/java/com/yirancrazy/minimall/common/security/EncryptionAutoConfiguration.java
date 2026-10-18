package com.yirancrazy.minimall.common.security;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: EncryptionAutoConfiguration 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@AutoConfiguration
public class EncryptionAutoConfiguration {

    /**
     * 执行 vaultKeyProvider 方法。
     * @param base64Key base64Key 参数
     * @return 处理结果
     */
    @Bean
    @ConditionalOnMissingBean(VaultKeyProvider.class)
    public VaultKeyProvider vaultKeyProvider(
            @Value("${minimall.security.aes-key:${MINIMALL_SECURITY_AES_KEY}}") String base64Key) {
        return new LocalVaultKeyProvider(base64Key);
    }

    /**
     * 执行 encryptionInitializer 方法。
     * @param keyProvider keyProvider 参数
     * @return 处理结果
     */
    @Bean
    public EncryptionInitializer encryptionInitializer(VaultKeyProvider keyProvider) {
        return new EncryptionInitializer(keyProvider);
    }


    /**
     * @Author: yirancrazy@gmail.com
     * @Description: EncryptionInitializer 类型，承载相关业务能力。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    static class EncryptionInitializer implements InitializingBean {

        private final VaultKeyProvider keyProvider;

        /**
         * 构造 EncryptionInitializer 实例。
         * @param keyProvider keyProvider
         */
        EncryptionInitializer(VaultKeyProvider keyProvider) {
            this.keyProvider = keyProvider;
        }

        /**
         * 执行 afterPropertiesSet 方法。
         */
        public void afterPropertiesSet() {
            EncryptionContext.initialize(keyProvider.getKey());
        }
    }
}
