package com.yirancrazy.minimall.platform.service.impl;

import java.util.Arrays;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.yirancrazy.minimall.common.constant.RoleEnum;
import com.yirancrazy.minimall.common.constant.RolePermissionMapping;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.platform.constant.PlatformCodeEnum;
import com.yirancrazy.minimall.platform.service.RolePermissionService;
import com.yirancrazy.minimall.platform.vo.PermissionVO;
import com.yirancrazy.minimall.platform.vo.RoleVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: RolePermissionServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Service
@RequiredArgsConstructor
public class RolePermissionServiceImpl implements RolePermissionService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 查询列表数据。
     * @return 结果列表
     */
    @Override
    public List<RoleVO> listRoles() {
        return Arrays.stream(RoleEnum.values())
            .map(this::toRoleVO)
            .toList();
    }

    /**
     * 获取相关数据。
     * @param roleCode roleCode 参数
     * @return 处理结果
     */
    @Override
    public RoleVO getRolePermissions(String roleCode) {
        RoleEnum role = RoleEnum.fromCode(roleCode);
        return role == null ? null : toRoleVO(role);
    }

    /**
     * 更新业务数据。
     * @param roleCode roleCode 参数
     * @param permissionCodes permissionCodes 参数
     */
    public void updatePermissions(String roleCode, List<String> permissionCodes) {
        RoleEnum role = RoleEnum.fromCode(roleCode);
        if (role == null) {
            throw new BizException(PlatformCodeEnum.ROLE_NOT_FOUND);
        }
        try {
            RolePermissionMapping.updatePermissions(role, permissionCodes, redisTemplate);
        }
        catch (IllegalArgumentException e) {
            if (e.getMessage().contains("不可修改")) {
                throw new BizException(PlatformCodeEnum.ROLE_PERMISSION_NOT_MODIFIABLE);
            }
            throw new BizException(PlatformCodeEnum.ROLE_PERMISSION_UPDATE_FAILED);
        }
    }

    /**
     * 转换或解析角色VO。
     * @param role 角色
     * @return 视图对象
     */
    private RoleVO toRoleVO(RoleEnum role) {
        List<PermissionVO> permissions = RolePermissionMapping.getEffectivePermissions(role, redisTemplate).stream()
            .map(p -> new PermissionVO(p.getCode(), p.getDescription()))
            .toList();
        return new RoleVO(role.getCode(), role.getDescription(), permissions);
    }
}
