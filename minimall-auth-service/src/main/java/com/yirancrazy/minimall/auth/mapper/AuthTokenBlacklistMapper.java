package com.yirancrazy.minimall.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.auth.entity.AuthTokenBlacklistPo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: t_auth_token_blacklist 的 MyBatis-Plus Mapper；供撤销记录 Manager 持久化 JWT 失效信息。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface AuthTokenBlacklistMapper extends BaseMapper<AuthTokenBlacklistPo> {}
