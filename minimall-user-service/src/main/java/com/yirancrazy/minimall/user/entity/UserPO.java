package com.yirancrazy.minimall.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class UserPO extends BasePO {
    private String username;
    private String nickname;
    private String phone;
    private String email;
}