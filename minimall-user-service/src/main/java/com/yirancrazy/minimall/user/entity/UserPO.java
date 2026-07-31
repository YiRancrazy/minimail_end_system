package com.yirancrazy.minimall.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserPO description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class UserPO extends BasePO {
    private String username;
    private String nickname;
    private String phone;
    private String email;
}