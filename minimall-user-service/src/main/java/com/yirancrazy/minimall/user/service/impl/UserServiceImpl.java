package com.yirancrazy.minimall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.user.UserManageVo;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDto;
import com.yirancrazy.minimall.common.constant.RoleEnum;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.common.util.SensitiveDataUtils;
import com.yirancrazy.minimall.user.constant.UserCodeEnum;
import com.yirancrazy.minimall.user.dto.UserPageDto;
import com.yirancrazy.minimall.user.dto.UserProfileDto;
import com.yirancrazy.minimall.user.dto.UserUpdateDto;
import com.yirancrazy.minimall.user.entity.UserPo;
import com.yirancrazy.minimall.user.manager.UserManager;
import com.yirancrazy.minimall.user.service.UserService;
import com.yirancrazy.minimall.user.vo.UserVo;
import java.security.SecureRandom;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户业务实现，负责建档幂等、游标分页、平台视图转换、用户资料局部更新以及敏感字段脱敏和头像 URL 解析.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final String NICKNAME_PREFIX = "薄荷用户";
    private static final SecureRandom NICKNAME_RANDOM = new SecureRandom();

    private final UserManager userManager;
    private final MinioUtil minioUtil;

    /**
     * 注入用户持久化和对象存储 URL 解析依赖。
     *
     * @param userManager 用户档案 Manager
     * @param minioUtil 头像 URL 解析工具
     */
    public UserServiceImpl(UserManager userManager, MinioUtil minioUtil) {
        this.userManager = userManager;
        this.minioUtil = minioUtil;
    }

    /**
     * 按用户 ID 查询用户持久化对象。
     *
     * @param id 用户 ID
     * @return 用户持久化对象
     * @throws BizException 用户不存在时抛出
     */
    public UserPo getById(Long id) {
        UserPo u = userManager.getById(id);
        if (u == null) {
            throw new BizException(UserCodeEnum.USER_NOT_FOUND);
        }
        return u;
    }

    /**
     * 构建供内部服务使用的用户快照。
     *
     * @param id 用户 ID
     * @return 包含用户 ID、昵称和 USER 角色编码的快照
     * @throws BizException 用户不存在时抛出
     */
    @Override
    public UserSnapshotDto getSnapshot(Long id) {
        UserPo user = getById(id);
        return new UserSnapshotDto(user.getId(), user.getNickname(), RoleEnum.USER.getCode());
    }

    /**
     * 在事务内为认证侧用户补齐本地档案。
     *
     * <p>以传入 ID 直接创建用户记录，生成“薄荷用户 + 六位随机数”昵称并将性别初始化为 0；
     * 已存在档案时直接返回，保证顺序调用幂等。
     *
     * @param userId 认证侧业务用户 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void provision(Long userId) {
        if (userManager.getById(userId) != null) {
            return;
        }
        UserPo user = new UserPo();
        user.setId(userId);
        user.setNickname(generateNickname());
        user.setGender(0);
        userManager.save(user);
    }

    /**
     * 平台侧更新用户昵称、手机号和邮箱。
     *
     * <p>手机号写入前生成脱敏值并保存到 phoneMasked，明文手机号和邮箱由持久化类型处理器加密。
     *
     * @param id 用户 ID
     * @param dto 用户资料更新参数
     * @return Manager 是否成功更新记录
     */
    @Override
    public boolean update(Long id, UserUpdateDto dto) {
        UserPo user = new UserPo();
        user.setId(id);
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setPhoneMasked(SensitiveDataUtils.maskPhone(dto.getPhone()));
        user.setEmail(dto.getEmail());
        return userManager.updateById(user);
    }

    /**
     * 按用户 ID 删除用户记录。
     *
     * @param id 用户 ID
     * @return Manager 是否成功删除记录
     */
    @Override
    public boolean delete(Long id) {
        return userManager.removeById(id);
    }

    /**
     * 使用游标分页查询用户持久化对象。
     *
     * <p>可选按昵称模糊过滤，并按主键倒序读取。
     *
     * @param dto 游标、每页数量和昵称关键字
     * @return 用户持久化游标分页结果
     */
    public CursorPageVo<UserPo> page(UserPageDto dto) {
        return pagePo(dto);
    }

    /**
     * 使用游标分页查询用户并转换为脱敏视图。
     *
     * <p>头像对象键通过 MinioUtil 解析为公开 URL。
     *
     * @param dto 游标、每页数量和昵称关键字
     * @return 用户视图游标分页结果
     */
    @Override
    public CursorPageVo<UserVo> pageVo(UserPageDto dto) {
        return pagePo(dto).map(po -> UserVo.from(po, minioUtil));
    }

    /**
     * 查询单个用户并转换为脱敏视图。
     *
     * @param id 用户 ID
     * @return 已脱敏并解析头像 URL 的用户视图
     * @throws BizException 用户不存在时抛出
     */
    @Override
    public UserVo getVo(Long id) {
        return UserVo.from(getById(id), minioUtil);
    }

    /**
     * 执行用户游标分页查询。
     *
     * <p>解码游标后按主键倒序读取 limit + 1 条记录，由 CursorPageVo 计算是否还有下一页。
     *
     * @param dto 游标、每页数量和可选昵称关键字
     * @return 用户持久化游标分页结果
     */
    private CursorPageVo<UserPo> pagePo(UserPageDto dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<UserPo> records =
                userManager.list(
                        Wrappers.lambdaQuery(UserPo.class)
                                .lt(lastId != null, UserPo::getId, lastId)
                                .and(
                                        dto.getKeyword() != null && !dto.getKeyword().isBlank(),
                                        w -> w.like(UserPo::getNickname, dto.getKeyword()))
                                .orderByDesc(UserPo::getId)
                                .last("LIMIT " + (limit + 1)));
        return CursorPageVo.of(records, limit, UserPo::getId);
    }

    /**
     * 为平台管理场景执行用户游标分页查询。
     *
     * <p>未指定 limit 时默认 20 条，支持昵称关键字过滤，并将持久化对象转换为用户管理视图。
     *
     * @param query 内部管理分页参数
     * @return 用户管理视图游标分页结果
     */
    @Override
    public CursorPageVo<UserManageVo> pageManage(InternalPageQuery query) {
        int limit = query.getLimit() == null ? 20 : query.getLimit();
        Long lastId = CursorUtils.decode(query.getCursor());
        LambdaQueryWrapper<UserPo> wrapper =
                Wrappers.lambdaQuery(UserPo.class)
                        .lt(lastId != null, UserPo::getId, lastId)
                        .and(
                                query.getKeyword() != null && !query.getKeyword().isBlank(),
                                w -> w.like(UserPo::getNickname, query.getKeyword()))
                        .orderByDesc(UserPo::getId)
                        .last("LIMIT " + (limit + 1));
        List<UserPo> records = userManager.list(wrapper);
        return CursorPageVo.of(records, limit, UserPo::getId).map(this::toUserManageVo);
    }

    /**
     * 查询平台用户管理详情。
     *
     * @param userId 目标用户 ID
     * @return 包含脱敏手机号的用户管理视图
     * @throws BizException 用户不存在时抛出
     */
    @Override
    public UserManageVo getManageDetail(Long userId) {
        UserPo user = userManager.getById(userId);
        if (user == null) {
            throw new BizException(UserCodeEnum.USER_NOT_FOUND);
        }
        return toUserManageVo(user);
    }

    /**
     * 查询当前用户资料持久化对象。
     *
     * @param userId 当前用户 ID
     * @return 用户持久化对象
     * @throws BizException 用户不存在时抛出
     */
    public UserPo getProfile(Long userId) {
        UserPo u = userManager.getById(userId);
        if (u == null) {
            throw new BizException(UserCodeEnum.USER_NOT_FOUND);
        }
        return u;
    }

    /**
     * 查询当前用户资料并转换为脱敏视图。
     *
     * @param userId 当前用户 ID
     * @return 当前用户视图
     * @throws BizException 用户不存在时抛出
     */
    @Override
    public UserVo getProfileVo(Long userId) {
        return UserVo.from(getProfile(userId), minioUtil);
    }

    /**
     * 局部更新当前用户资料，仅写入 DTO 中非 null 的字段。
     *
     * @param userId 当前用户 ID
     * @param dto 昵称、头像和性别的局部更新参数
     * @return Manager 是否成功更新记录
     */
    @Override
    public boolean updateProfile(Long userId, UserProfileDto dto) {
        UserPo user = new UserPo();
        user.setId(userId);
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        boolean ok = userManager.updateById(user);
        log.info("用户资料已更新，userId={}", userId);
        return ok;
    }

    /**
     * 将用户持久化对象转换为平台用户管理视图。
     *
     * <p>视图使用数据库中保存的 phoneMasked，避免向管理端返回手机号明文。
     *
     * @param user 用户持久化对象
     * @return 用户管理视图
     */
    private UserManageVo toUserManageVo(UserPo user) {
        return new UserManageVo(
                user.getId(),
                user.getNickname(),
                user.getAvatar(),
                user.getPhoneMasked(),
                user.getGender(),
                user.getCreateTime());
    }

    /**
     * 生成默认用户昵称。
     *
     * <p>使用进程级 SecureRandom 追加六位数字；该方法不检查昵称全局唯一性。
     *
     * @return 形如“薄荷用户123456”的昵称
     */
    private String generateNickname() {
        return String.format("%s%06d", NICKNAME_PREFIX, NICKNAME_RANDOM.nextInt(1_000_000));
    }
}
