package com.yirancrazy.minimall.api.dto.user;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserManageVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserManageVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;

    private String phoneMasked;
    private Integer gender;
    private LocalDateTime createTime;
}
