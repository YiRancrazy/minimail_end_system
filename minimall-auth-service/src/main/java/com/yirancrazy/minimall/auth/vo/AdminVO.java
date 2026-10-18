package com.yirancrazy.minimall.auth.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AdminVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@AllArgsConstructor
public class AdminVO {
    private Long id;
    private String account;
    private String nickname;
    private Integer status;
    private LocalDateTime createTime;
    private String role;
    private String contact;
}
