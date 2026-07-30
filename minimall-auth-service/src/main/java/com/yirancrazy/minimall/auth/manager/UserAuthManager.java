package com.yirancrazy.minimall.auth.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.auth.entity.UserAuthPO;

/**
* 认证数据访问管理接口，封装 UserAuthPO 持久化与按用户名查询能力。
 */
public interface UserAuthManager extends IService<UserAuthPO> {
}