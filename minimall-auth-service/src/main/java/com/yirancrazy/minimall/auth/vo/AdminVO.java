package com.yirancrazy.minimall.auth.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台管理员视图对象，用于管理员列表输出
 * @Version: 1.0
 * @DateTime: 2026/08/04
 */
@Data
@AllArgsConstructor
public class AdminVO {
    private Long id;
    private String account;
    private String nickname;
    private Integer status;
    private LocalDateTime createTime;
}
