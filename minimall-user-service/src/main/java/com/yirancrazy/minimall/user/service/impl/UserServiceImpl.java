package com.yirancrazy.minimall.user.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.user.UserManageVO;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDTO;
import com.yirancrazy.minimall.common.constant.RoleEnum;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.common.util.SensitiveDataUtils;
import com.yirancrazy.minimall.user.constant.UserCodeEnum;
import com.yirancrazy.minimall.user.dto.UserCreateDTO;
import com.yirancrazy.minimall.user.dto.UserPageDTO;
import com.yirancrazy.minimall.user.dto.UserProfileDTO;
import com.yirancrazy.minimall.user.dto.UserUpdateDTO;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.manager.UserManager;
import com.yirancrazy.minimall.user.service.UserService;
import com.yirancrazy.minimall.user.vo.UserVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserManager userManager;
    private final MinioUtil minioUtil;

    /**
     * 构造 UserServiceImpl 实例。
     * @param userManager userManager 参数
     * @param minioUtil minioUtil 参数
     */
    public UserServiceImpl(UserManager userManager, MinioUtil minioUtil) {
        this.userManager = userManager;
        this.minioUtil = minioUtil;
    }


    /**
     * 获取相关数据。
     * @param id id 参数
     * @return 处理结果
     */
    public UserPO getById(Long id) {
        UserPO u = userManager.getById(id);
        if (u == null) {
            throw new BizException(UserCodeEnum.USER_NOT_FOUND);
        }
        return u;
    }


    /**
     * 获取相关数据。
     * @param id id 参数
     * @return 处理结果
     */
    @Override
    public UserSnapshotDTO getSnapshot(Long id) {
        UserPO user = getById(id);
        return new UserSnapshotDTO(user.getId(), user.getUsername(), RoleEnum.USER.getCode());
    }


    /**
     * 创建业务数据。
     * @param dto dto 参数
     * @return 业务数值
     */
    @Override
    public Long create(UserCreateDTO dto) {
        UserPO user = new UserPO();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setPhoneMasked(SensitiveDataUtils.maskPhone(dto.getPhone()));
        user.setEmail(dto.getEmail());
        userManager.save(user);
        return user.getId();
    }


    /**
     * 更新业务数据。
     * @param id id 参数
     * @param dto dto 参数
     * @return 是否处理成功
     */
    @Override
    public boolean update(Long id, UserUpdateDTO dto) {
        UserPO user = new UserPO();
        user.setId(id);
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setPhoneMasked(SensitiveDataUtils.maskPhone(dto.getPhone()));
        user.setEmail(dto.getEmail());
        return userManager.updateById(user);
    }


    /**
     * 删除业务数据。
     * @param id id 参数
     * @return 是否处理成功
     */
    @Override
    public boolean delete(Long id) {
        return userManager.removeById(id);
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    public CursorPageVO<UserPO> page(UserPageDTO dto) {
        return pagePO(dto);
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<UserVO> pageVO(UserPageDTO dto) {
        return pagePO(dto).map(po -> UserVO.from(po, minioUtil));
    }


    /**
     * 获取相关数据。
     * @param id id 参数
     * @return 处理结果
     */
    @Override
    public UserVO getVO(Long id) {
        return UserVO.from(getById(id), minioUtil);
    }


    /**
     * 查询PO。
     * @param dto 请求参数
     * @return 分页结果
     */
    private CursorPageVO<UserPO> pagePO(UserPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<UserPO> records = userManager.list(Wrappers.lambdaQuery(UserPO.class)
            .lt(lastId != null, UserPO::getId, lastId)
            .and(dto.getKeyword() != null && !dto.getKeyword().isBlank(),
                w -> w.like(UserPO::getUsername, dto.getKeyword())
                    .or().like(UserPO::getNickname, dto.getKeyword()))
            .orderByDesc(UserPO::getId)
            .last("LIMIT " + (limit + 1)));
        return CursorPageVO.of(records, limit, UserPO::getId);
    }


    /**
     * 分页查询数据。
     * @param query query 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<UserManageVO> pageManage(InternalPageQuery query) {
        int limit = query.getLimit() == null ? 20 : query.getLimit();
        Long lastId = CursorUtils.decode(query.getCursor());
        LambdaQueryWrapper<UserPO> wrapper = Wrappers.lambdaQuery(UserPO.class)
            .lt(lastId != null, UserPO::getId, lastId)
            .and(query.getKeyword() != null && !query.getKeyword().isBlank(),
                w -> w.like(UserPO::getUsername, query.getKeyword())
                    .or().like(UserPO::getNickname, query.getKeyword()))
            .orderByDesc(UserPO::getId)
            .last("LIMIT " + (limit + 1));
        List<UserPO> records = userManager.list(wrapper);
        return CursorPageVO.of(records, limit, UserPO::getId).map(this::toUserManageVO);
    }


    /**
     * 获取相关数据。
     * @param userId userId 参数
     * @return 处理结果
     */
    @Override
    public UserManageVO getManageDetail(Long userId) {
        UserPO user = userManager.getById(userId);
        if (user == null) {
            throw new BizException(UserCodeEnum.USER_NOT_FOUND);
        }
        return toUserManageVO(user);
    }


    /**
     * 获取相关数据。
     * @param userId userId 参数
     * @return 处理结果
     */
    public UserPO getProfile(Long userId) {
        UserPO u = userManager.getById(userId);
        if (u == null) {
            throw new BizException(UserCodeEnum.USER_NOT_FOUND);
        }
        return u;
    }


    /**
     * 获取相关数据。
     * @param userId userId 参数
     * @return 处理结果
     */
    @Override
    public UserVO getProfileVO(Long userId) {
        return UserVO.from(getProfile(userId), minioUtil);
    }


    /**
     * 更新业务数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 是否处理成功
     */
    @Override
    public boolean updateProfile(Long userId, UserProfileDTO dto) {
        UserPO user = new UserPO();
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
     * 转换或解析用户ManageVO。
     * @param user 用户
     * @return 视图对象
     */
    private UserManageVO toUserManageVO(UserPO user) {
        return new UserManageVO(user.getId(), user.getUsername(), user.getNickname(),
                user.getAvatar(), user.getPhoneMasked(), user.getGender(), user.getCreateTime());
    }
}
