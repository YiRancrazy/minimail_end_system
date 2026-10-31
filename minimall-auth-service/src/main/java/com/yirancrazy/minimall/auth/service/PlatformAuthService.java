package com.yirancrazy.minimall.auth.service;

import com.yirancrazy.minimall.api.dto.auth.TokenVo;
import com.yirancrazy.minimall.auth.dto.AdminCreateDto;
import com.yirancrazy.minimall.auth.dto.AdminPageDto;
import com.yirancrazy.minimall.auth.dto.AdminRoleDto;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDto;
import com.yirancrazy.minimall.auth.dto.LoginDto;
import com.yirancrazy.minimall.auth.vo.AdminVo;
import com.yirancrazy.minimall.auth.vo.UserInfoVo;
import com.yirancrazy.minimall.common.result.CursorPageVo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台管理员认证与管理契约；覆盖管理员登录、会话、分页、角色分配和删除规则。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface PlatformAuthService {

  /**
   * 校验平台管理员账号类型、启用状态和密码，更新最近登录时间后签发令牌。
   *
   * @param dto 管理员登录账号和密码
   * @return 新签发的令牌
   * @throws com.yirancrazy.minimall.common.exception.BizException 账号不存在、类型不匹配、禁用或密码错误时抛出
   */
  TokenVo login(LoginDto dto);

  /**
   * 退出管理员会话，清除刷新令牌并撤销当前 JWT。
   *
   * @param adminAccountId 管理员认证账号 ID
   * @param jti 当前 JWT 唯一标识
   */
  void signOut(Long adminAccountId, String jti);

  /**
   * 创建平台管理员；角色编码为空时使用平台默认角色。
   *
   * @param dto 管理员账号、密码、可选联系方式和角色编码
   * @return 新建管理员认证账号 ID
   * @throws com.yirancrazy.minimall.common.exception.BizException 账号重复或角色不存在时抛出
   */
  Long adminCreate(AdminCreateDto dto);

  /**
   * 按认证账号 ID 倒序查询平台管理员，并转换为不包含凭据的视图。
   *
   * @param dto 游标和每页数量
   * @return 管理员游标分页结果
   */
  CursorPageVo<AdminVo> adminPage(AdminPageDto dto);

  /**
   * 将指定管理员账号绑定到角色编码对应的持久化角色。
   *
   * @param id 目标管理员认证账号 ID
   * @param dto 新角色编码
   * @throws com.yirancrazy.minimall.common.exception.BizException 管理员或角色不存在时抛出
   */
  void adminAssignRole(Long id, AdminRoleDto dto);

  /**
   * 校验旧密码后更新管理员密码，并使该账号的刷新令牌全部失效。
   *
   * @param adminAccountId 管理员认证账号 ID
   * @param dto 旧密码和新密码
   * @throws com.yirancrazy.minimall.common.exception.BizException 账号不存在或旧密码错误时抛出
   */
  void changePassword(Long adminAccountId, ChangePasswordDto dto);

  /**
   * 删除指定管理员，并禁止操作者删除自身。
   *
   * @param operatorId 当前操作者账号 ID
   * @param targetId 待删除管理员账号 ID
   * @throws com.yirancrazy.minimall.common.exception.BizException 删除自身或目标不存在时抛出
   */
  void adminDelete(Long operatorId, Long targetId);

  /**
   * 校验并解析 JWT，返回当前平台管理员的安全身份信息。
   *
   * @param token 不带 Bearer 前缀的 JWT
   * @return 管理员认证账号 ID、账号和角色信息
   * @throws com.yirancrazy.minimall.common.exception.BizException JWT 无效或过期时抛出
   */
  UserInfoVo me(String token);
}
