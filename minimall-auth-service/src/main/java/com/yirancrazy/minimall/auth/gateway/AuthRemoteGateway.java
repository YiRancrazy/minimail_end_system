package com.yirancrazy.minimall.auth.gateway;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 认证服务的远程调用边界；隔离用户注册所需的 ID 生成和用户档案创建依赖。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
public interface AuthRemoteGateway {

  /**
   * 向 ID 服务申请一个新的业务用户 ID。
   *
   * @return 业务用户 ID
   * @throws com.yirancrazy.minimall.common.exception.BizException 远程调用失败或返回空 ID 时抛出
   */
  Long nextUserId();

  /**
   * 请求用户服务为指定业务用户 ID 创建档案。
   *
   * @param userId 业务用户 ID
   * @throws com.yirancrazy.minimall.common.exception.BizException 远程调用失败时抛出
   */
  void provisionUserProfile(Long userId);
}
