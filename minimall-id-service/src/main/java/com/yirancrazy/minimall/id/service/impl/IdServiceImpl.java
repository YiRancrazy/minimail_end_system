package com.yirancrazy.minimall.id.service.impl;

import com.yirancrazy.minimall.id.service.IdService;
import com.yirancrazy.minimall.id.service.Snowflake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdService 的 Snowflake 实现。构造时从 Spring 配置或环境变量回退值解析
 *     数据中心和工作节点编号，并让所有业务标签共享同一个生成器；不使用数据库号段或 bizTag。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Service
public class IdServiceImpl implements IdService {

  private final Snowflake snowflake;

  /**
   * 创建服务并初始化唯一的 Snowflake 生成器。
   *
   * <p>配置值会先 trim 再按十进制 long 解析。缺失值抛出 IllegalStateException；
   * 解析后的节点编号超出 [0, 31] 时抛出 IllegalArgumentException。
   *
   * @param datacenterId 来自 minimall.id.datacenter-id 或环境变量的数据中心编号
   * @param workerId 来自 minimall.id.worker-id 或环境变量的工作节点编号
   * @throws IllegalStateException 配置值为空白或不是合法 long 时抛出
   * @throws IllegalArgumentException 解析后的节点编号超出 [0, 31] 时抛出
   */
  public IdServiceImpl(
      @Value("${minimall.id.datacenter-id:${SNOWFLAKE_DATACENTER_ID:}}") String datacenterId,
      @Value("${minimall.id.worker-id:${SNOWFLAKE_WORKER_ID:}}") String workerId) {
    this.snowflake =
        new Snowflake(
            parseNodeId(datacenterId, "datacenter-id"), parseNodeId(workerId, "worker-id"));
  }

  /**
   * 忽略传入的业务标签并返回下一个 Snowflake ID。
   *
   * <p>标签可以为 null，当前实现会主动忽略；每次调用都会推进共享生成器，序列耗尽时可能
   * 阻塞到下一毫秒。
   *
   * @param bizTag 调用方传入但当前被忽略的业务标签
   * @return 下一个生成的 Snowflake ID
   * @throws IllegalStateException Snowflake 检测到时钟回拨时抛出
   */
  @Override
  public long nextId(String bizTag) {
    return snowflake.nextId();
  }

  /**
   * 将配置的 Snowflake 节点编号解析为十进制 long。
   *
   * @param value 原始配置值，解析前会先执行 trim
   * @param name 用于错误消息的配置键后缀
   * @return 尚未经过 Snowflake 范围校验的节点编号
   * @throws IllegalStateException 配置值为空白或不是合法十进制 long 时抛出
   */
  private static long parseNodeId(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("缺少配置项 minimall.id." + name);
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      throw new IllegalStateException("配置项 minimall.id." + name + " 非法：" + value, e);
    }
  }
}
