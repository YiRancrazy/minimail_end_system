package com.yirancrazy.minimall.auth.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.auth.entity.AuthTokenBlacklistPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthTokenBlacklist数据访问层接口，定义Token黑名单表操作契约
 * @Version: 1.0
 * @DateTime: 2026/08/04
 */
public interface AuthTokenBlacklistManager extends IService<AuthTokenBlacklistPO> {
}
