package com.yirancrazy.minimall.user.vo;

import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.common.util.SensitiveDataUtils;
import com.yirancrazy.minimall.user.entity.UserPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private Integer gender;
    private LocalDateTime createTime;


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
     */
    public static UserVO from(UserPO po) {
        return from(po, null);
    }


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @param minioUtil minioUtil 参数
     * @return 处理结果
     */
    public static UserVO from(UserPO po, MinioUtil minioUtil) {
        UserVO vo = new UserVO();
        vo.setId(po.getId());
        vo.setUsername(po.getUsername());
        vo.setNickname(po.getNickname());
        vo.setPhone(SensitiveDataUtils.maskPhone(po.getPhone()));
        vo.setEmail(maskEmail(po.getEmail()));
        vo.setAvatar(minioUtil == null ? po.getAvatar() : minioUtil.resolvePublicUrl(po.getAvatar()));
        vo.setGender(po.getGender());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }


    /**
     * 处理maskEmail。
     * @param email email
     * @return 处理结果
     */
    private static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        int keep = Math.min(2, atIndex);
        return email.substring(0, keep) + "***" + email.substring(atIndex);
    }
}
