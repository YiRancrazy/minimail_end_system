package com.yirancrazy.minimall.common.security;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 保存应用级 AES 密钥的内存上下文，初始化时复制密钥，读取时再次返回副本，
 * 避免调用方修改内部密钥数组。未初始化时读取会立即失败。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public final class EncryptionContext {

  private static volatile byte[] key;

  /** 加密上下文工具类禁止实例化。 */
  private EncryptionContext() {}

  /**
   * 保存密钥副本。该方法仅由启动初始化器调用，后续覆盖会替换全局密钥。
   *
   * @param aesKey 32 字节 AES-256 密钥
   */
  static void initialize(byte[] aesKey) {
    key = aesKey.clone();
  }

  /**
   * 返回当前密钥的副本，防止调用方直接修改共享密钥。
   *
   * @return AES-256 密钥副本
   * @throws IllegalStateException 上下文尚未初始化时抛出
   */
  public static byte[] getKey() {
    if (key == null) {
      throw new IllegalStateException("EncryptionContext 未初始化");
    }
    return key.clone();
  }
}
