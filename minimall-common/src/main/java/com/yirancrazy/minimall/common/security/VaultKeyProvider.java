package com.yirancrazy.minimall.common.security;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 密钥提供者抽象，用于从本地配置或外部密钥管理系统获取 AES-256 密钥。
 * 实现必须返回 32 字节密钥，并可自行决定是否返回防御性副本。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface VaultKeyProvider {

  /**
   * 返回当前 AES-256 密钥。
   *
   * @return 32 字节密钥
   */
  byte[] getKey();
}
