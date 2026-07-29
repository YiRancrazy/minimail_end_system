package com.yirancrazy.minimall.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户持久化实体，承载用户标识、用户名、昵称、联系方式等档案字段。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class UserPO extends BasePO {
    private String username;
    private String nickname;
    private String phone;
    private String email;
}