package com.yirancrazy.minimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserAuth持久化对象，映射userauth表
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Data
public class UserAuthPO extends BasePO {
    private String username;
    private String passwordHash;
    private String salt;
    private String role;
    private Integer status;
}