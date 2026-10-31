package com.yirancrazy.minimall.id.service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 调用方获取一个 long 型 ID 的服务契约。实现类负责生成策略和状态；
 *     当前实现使用 Snowflake，并将 bizTag 仅作为调用元数据。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface IdService {

  /**
   * 返回下一个标识符并推进底层生成器状态。
   *
   * <p>当前实现不会校验或根据 bizTag 分支，因此该参数可以为 null，且不会影响生成的序列。
   *
   * @param bizTag 调用方传入并用于表达调用上下文的业务标签
   * @return 下一个生成的 long 型标识符
   * @throws IllegalStateException 实现检测到时钟回拨时抛出
   */
  long nextId(String bizTag);
}
