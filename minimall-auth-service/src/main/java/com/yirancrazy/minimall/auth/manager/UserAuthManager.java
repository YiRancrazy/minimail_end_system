package com.yirancrazy.minimall.auth.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.auth.entity.UserAuthPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 认证数据访问管理接口，封装 UserAuthPO 持久化与按用户名查询能力。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public interface UserAuthManager extends IService<UserAuthPO> {
}