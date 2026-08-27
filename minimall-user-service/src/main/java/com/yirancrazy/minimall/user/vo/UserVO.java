package com.yirancrazy.minimall.user.vo;

import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.common.util.SensitiveDataUtils;
import com.yirancrazy.minimall.user.entity.UserPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户VO，用于Controller边界输出，隐藏内部字段并对敏感数据脱敏
 * @Version: 1.0
 * @DateTime: 2026/08/04
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
     * 将 UserPO 转换为 UserVO，对手机号和邮箱进行脱敏处理。
     * @param po 用户持久化对象
     * @return 脱敏后的用户VO
     */
    public static UserVO from(UserPO po) {
        return from(po, null);
    }

    /**
     * 将 UserPO 转换为 UserVO，对手机号和邮箱进行脱敏，并将头像 objectKey 解析为可访问 URL。
     * @param po 用户持久化对象
     * @param minioUtil 对象存储工具，用于生成头像预签名 URL；为 null 时头像原样返回
     * @return 脱敏后的用户VO
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
     * 邮箱脱敏：保留@前缀前至少1个字符，其余前缀用*替换后保留@域名后缀。
     * @param email 原始邮箱
     * @return 脱敏后的邮箱
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
