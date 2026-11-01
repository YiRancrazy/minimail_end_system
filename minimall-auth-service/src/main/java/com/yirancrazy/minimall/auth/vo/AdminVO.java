package com.yirancrazy.minimall.auth.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台管理员列表视图，仅暴露账号、状态、角色和联系方式，不包含密码散列或盐值。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
public class AdminVo {
    private Long id;
    private String account;
    private Integer status;
    private LocalDateTime createTime;
    private String role;
    private String contact;
}
