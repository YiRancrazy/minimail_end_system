package com.yirancrazy.minimall.platform.service;

import java.util.List;
import com.yirancrazy.minimall.platform.vo.RoleVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: RolePermissionService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface RolePermissionService {


    /**
     * 查询列表数据。
     * @return 结果列表
     */
    List<RoleVO> listRoles();


    /**
     * 获取相关数据。
     * @param roleCode roleCode 参数
     * @return 处理结果
     */
    RoleVO getRolePermissions(String roleCode);


    /**
     * 更新业务数据。
     * @param roleCode roleCode 参数
     * @param permissionCodes permissionCodes 参数
     */
    void updatePermissions(String roleCode, List<String> permissionCodes);
}
