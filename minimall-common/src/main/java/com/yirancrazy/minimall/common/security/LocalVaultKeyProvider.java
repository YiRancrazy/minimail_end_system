package com.yirancrazy.minimall.common.security;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: LocalVaultKeyProvider 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class LocalVaultKeyProvider implements VaultKeyProvider {

    private final byte[] key;


    /**
     * 构造 LocalVaultKeyProvider 实例。
     * @param base64Key base64Key 参数
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
     * 获取相关数据。
     * @return 结果列表
     */
    @Override
    public byte[] getKey() {
        return key.clone();
    }
}
