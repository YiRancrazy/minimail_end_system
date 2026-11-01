package com.yirancrazy.minimall.user.service;

import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.user.UserManageVo;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDto;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.user.dto.UserPageDto;
import com.yirancrazy.minimall.user.dto.UserProfileDto;
import com.yirancrazy.minimall.user.dto.UserUpdateDto;
import com.yirancrazy.minimall.user.entity.UserPo;
import com.yirancrazy.minimall.user.vo.UserVo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户业务接口，覆盖建档、内部快照、平台管理查询、用户自助资料查询与局部更新等能力.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface UserService {

    /**
     * 按用户 ID 查询用户持久化对象。
     *
     * @param id 用户 ID
     * @return 用户持久化对象
     * @throws com.yirancrazy.minimall.common.exception.BizException 用户不存在时抛出
     */
    UserPo getById(Long id);

    /**
     * 构建内部服务使用的用户快照。
     *
     * @param id 用户 ID
     * @return 包含用户 ID、昵称和 USER 角色编码的快照
     * @throws com.yirancrazy.minimall.common.exception.BizException 用户不存在时抛出
     */
    UserSnapshotDto getSnapshot(Long id);

    /**
     * 按游标分页查询用户持久化对象。
     *
     * @param dto 游标、每页数量和昵称关键字
     * @return 按主键倒序排列的用户持久化分页结果
     */
    CursorPageVo<UserPo> page(UserPageDto dto);

    /**
     * 为认证侧已创建的业务用户补齐本地档案。
     *
     * <p>以调用方提供的用户 ID 建库；档案已存在时不写入或更新，保证顺序调用幂等，写入在事务中完成。
     *
     * @param userId 业务用户 ID
     */
    void provision(Long userId);

    /**
     * 平台侧更新用户的昵称、手机号和邮箱。
     *
     * <p>手机号密文和脱敏值同时写入，返回值直接反映 Manager 更新结果。
     *
     * @param id 用户 ID
     * @param dto 更新字段
     * @return 是否成功更新记录
     */
    boolean update(Long id, UserUpdateDto dto);

    /**
     * 按用户 ID 删除用户记录。
     *
     * @param id 用户 ID
     * @return 是否成功删除记录
     */
    boolean delete(Long id);

    /**
     * 分页查询用户并转换为接口视图。
     *
     * <p>转换过程对手机号和邮箱脱敏，并将头像对象键解析为公开 URL。
     *
     * @param dto 游标、每页数量和昵称关键字
     * @return 用户视图游标分页结果
     */
    CursorPageVo<UserVo> pageVo(UserPageDto dto);

    /**
     * 按用户 ID 查询脱敏后的用户视图。
     *
     * @param id 用户 ID
     * @return 用户视图
     * @throws com.yirancrazy.minimall.common.exception.BizException 用户不存在时抛出
     */
    UserVo getVo(Long id);

    /**
     * 为平台管理场景分页查询用户。
     *
     * <p>未指定 limit 时默认每页 20 条，支持昵称关键字过滤，并按主键倒序返回管理视图。
     *
     * @param query 内部管理分页参数
     * @return 用户管理视图分页结果
     */
    CursorPageVo<UserManageVo> pageManage(InternalPageQuery query);

    /**
     * 查询当前用户资料持久化对象。
     *
     * @param userId 当前用户 ID
     * @return 用户持久化对象
     * @throws com.yirancrazy.minimall.common.exception.BizException 用户不存在时抛出
     */
    UserPo getProfile(Long userId);

    /**
     * 查询平台用户管理详情。
     *
     * @param userId 目标用户 ID
     * @return 包含脱敏手机号的用户管理视图
     * @throws com.yirancrazy.minimall.common.exception.BizException 用户不存在时抛出
     */
    UserManageVo getManageDetail(Long userId);

    /**
     * 查询当前用户资料视图。
     *
     * <p>返回前完成手机号和邮箱脱敏以及头像 URL 解析。
     *
     * @param userId 当前用户 ID
     * @return 当前用户视图
     * @throws com.yirancrazy.minimall.common.exception.BizException 用户不存在时抛出
     */
    UserVo getProfileVo(Long userId);

    /**
     * 局部更新当前用户资料。
     *
     * <p>仅更新 DTO 中非 null 的昵称、头像和性别字段，允许其他资料保持不变。
     *
     * @param userId 当前用户 ID
     * @param dto 资料局部更新参数
     * @return 是否成功更新记录
     */
    boolean updateProfile(Long userId, UserProfileDto dto);
}
