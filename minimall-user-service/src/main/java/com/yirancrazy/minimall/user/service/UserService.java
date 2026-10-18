package com.yirancrazy.minimall.user.service;

import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.user.UserManageVO;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDTO;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.user.dto.UserCreateDTO;
import com.yirancrazy.minimall.user.dto.UserPageDTO;
import com.yirancrazy.minimall.user.dto.UserProfileDTO;
import com.yirancrazy.minimall.user.dto.UserUpdateDTO;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.vo.UserVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface UserService {


    /**
     * 获取相关数据。
     * @param id id 参数
     * @return 处理结果
     */
    UserPO getById(Long id);
    /**
     * 获取相关数据。
     * @param id id 参数
     * @return 处理结果
     */
    UserSnapshotDTO getSnapshot(Long id);


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<UserPO> page(UserPageDTO dto);
    /**
     * 创建业务数据。
     * @param dto dto 参数
     * @return 业务数值
     */
    Long create(UserCreateDTO dto);


    /**
     * 更新业务数据。
     * @param id id 参数
     * @param dto dto 参数
     * @return 是否处理成功
     */
    boolean update(Long id, UserUpdateDTO dto);


    /**
     * 删除业务数据。
     * @param id id 参数
     * @return 是否处理成功
     */
    boolean delete(Long id);



    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<UserVO> pageVO(UserPageDTO dto);


    /**
     * 获取相关数据。
     * @param id id 参数
     * @return 处理结果
     */
    UserVO getVO(Long id);


    /**
     * 分页查询数据。
     * @param query query 参数
     * @return 处理结果
     */
    CursorPageVO<UserManageVO> pageManage(InternalPageQuery query);


    /**
     * 获取相关数据。
     * @param userId userId 参数
     * @return 处理结果
     */
    UserPO getProfile(Long userId);
    /**
     * 获取相关数据。
     * @param userId userId 参数
     * @return 处理结果
     */
    UserManageVO getManageDetail(Long userId);



    /**
     * 获取相关数据。
     * @param userId userId 参数
     * @return 处理结果
     */
    UserVO getProfileVO(Long userId);


    /**
     * 更新业务数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 是否处理成功
     */
    boolean updateProfile(Long userId, UserProfileDTO dto);
}
