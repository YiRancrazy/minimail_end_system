package com.yirancrazy.minimall.user.vo;

import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.common.util.SensitiveDataUtils;
import com.yirancrazy.minimall.user.entity.UserPo;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户接口视图，对手机号和邮箱脱敏，并在提供 MinioUtil 时将头像对象键解析为可访问 URL.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class UserVo {

    private Long id;
    private String nickname;
    private String phone;
    private String email;
    private String avatar;
    private Integer gender;
    private LocalDateTime createTime;

    /**
     * 将用户持久化对象转换为接口视图，不解析头像对象键。
     *
     * @param po 用户持久化对象
     * @return 已完成手机号和邮箱脱敏、头像保持原值的用户视图
     */
    public static UserVo from(UserPo po) {
        return from(po, null);
    }

    /**
     * 将用户持久化对象转换为接口视图，并对敏感信息做展示脱敏。
     *
     * <p>手机号由 SensitiveDataUtils 脱敏，邮箱保留 @ 前最多两个字符并隐藏其余前缀；提供 MinioUtil 时，
     * 头像对象键会解析为公开 URL。
     *
     * @param po 用户持久化对象
     * @param minioUtil MinIO URL 解析工具；为 null 时保留原始头像值
     * @return 已完成敏感字段处理和头像解析的用户视图
     */
    public static UserVo from(UserPo po, MinioUtil minioUtil) {
        UserVo vo = new UserVo();
        vo.setId(po.getId());
        vo.setNickname(po.getNickname());
        vo.setPhone(SensitiveDataUtils.maskPhone(po.getPhone()));
        vo.setEmail(maskEmail(po.getEmail()));
        vo.setAvatar(minioUtil == null ? po.getAvatar() : minioUtil.resolvePublicUrl(po.getAvatar()));
        vo.setGender(po.getGender());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }

    /**
     * 对邮箱前缀做展示脱敏。
     *
     * <p>空值和缺少有效 @ 前缀的值原样返回；正常邮箱保留 @ 前最多两个字符，其余前缀替换为 ***。
     *
     * @param email 原始邮箱
     * @return 脱敏后的邮箱，或无法识别时原样返回的值
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
