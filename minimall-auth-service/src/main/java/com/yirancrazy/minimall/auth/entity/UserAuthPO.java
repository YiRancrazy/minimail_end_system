package com.yirancrazy.minimall.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserAuth持久化对象，映射userauth表
 * @Version: 1.1
 * @DateTime: 2026/08/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user_auth")
public class UserAuthPO extends BasePO {
    private String username;
    private String passwordHash;
    private String salt;
    private String role;
    private Integer status;
    private String nickname;
}
