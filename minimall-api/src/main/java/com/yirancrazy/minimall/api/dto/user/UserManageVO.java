package com.yirancrazy.minimall.api.dto.user;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户管理 VO，仅暴露非敏感字段，供跨服务调用使用
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserManageVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    /** 仅返回脱敏值，不返回加密的 phone 字段 */
    private String phoneMasked;
    private Integer gender;
    private LocalDateTime createTime;
}
