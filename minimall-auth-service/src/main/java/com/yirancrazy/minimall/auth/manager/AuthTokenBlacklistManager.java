package com.yirancrazy.minimall.auth.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.auth.entity.AuthTokenBlacklistPo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: JWT 撤销记录数据访问边界，提供 t_auth_token_blacklist 的持久化查询与写入能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface AuthTokenBlacklistManager extends IService<AuthTokenBlacklistPo> {}
