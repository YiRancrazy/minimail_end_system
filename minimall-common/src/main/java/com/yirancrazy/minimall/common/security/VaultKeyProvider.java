package com.yirancrazy.minimall.common.security;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Vault密钥提供者接口，定义获取AES-256加密密钥的契约。生产环境由HashiCorp Vault实现，开发环境由LocalVaultKeyProvider实现。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
public interface VaultKeyProvider {

    /**
     * 获取AES-256加密密钥（32字节）。
     *
     * @return 256位加密密钥的字节数组
     */
    byte[] getKey();
}
