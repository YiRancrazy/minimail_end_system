package com.yirancrazy.minimall.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 个人资料更新 DTO，仅允许修改昵称、头像、性别
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class UserProfileDTO {

    @Size(max = 100, message = "nickname length must be <= 100")
    private String nickname;

    @Size(max = 512, message = "avatar length must be <= 512")
    private String avatar;

    /** 性别 0=未知 1=男 2=女；null 不更新 */
    private Integer gender;
}
