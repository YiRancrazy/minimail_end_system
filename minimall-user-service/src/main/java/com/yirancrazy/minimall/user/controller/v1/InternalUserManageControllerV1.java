package com.yirancrazy.minimall.user.controller.v1;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.user.UserManageVO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.constant.UserCodeEnum;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.manager.UserManager;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户管理内部控制器（仅内网调用），供平台服务远程调用。
 *              不使用 user-service 现有 UserService 是为了避免泄露业务校验（如 paramInvalid 等），
 *              且不需要走 Service 层业务封装。
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@RestController
@RequestMapping("/internal/user/manage")
public class InternalUserManageControllerV1 {

    private final UserManager userManager;

    public InternalUserManageControllerV1(UserManager userManager) {
        this.userManager = userManager;
    }

    /**
     * 平台分页查询用户列表。
     * Internal: 仅内网调用，禁止 Gateway 暴露。
     * @param query 通用分页查询
     * @return 用户管理分页结果
     */
    @GetMapping
    public Result<CursorPageVO<UserManageVO>> page(InternalPageQuery query) {
        int limit = query.getLimit() == null ? 20 : query.getLimit();
        LambdaQueryWrapper<UserPO> wrapper = Wrappers.lambdaQuery(UserPO.class);
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword();
            wrapper.and(w -> w.like(UserPO::getUsername, kw)
                    .or().like(UserPO::getNickname, kw));
        }
        wrapper.orderByDesc(UserPO::getId);
        Page<UserPO> mpPage = Page.of(1, limit + 1);
        Page<UserPO> result = userManager.page(mpPage, wrapper);
        List<UserPO> rows = result.getRecords();
        boolean hasMore = rows.size() > limit;
        List<UserPO> pageRecords = hasMore ? rows.subList(0, limit) : rows;
        List<UserManageVO> records = pageRecords.stream().map(this::toVO).toList();
        CursorPageVO<UserManageVO> vo = CursorPageVO.of(
                new java.util.ArrayList<>(records), limit, UserManageVO::getId);
        return Result.success(vo);
    }

    /**
     * 平台查询用户详情。
     * Internal: 仅内网调用，禁止 Gateway 暴露。
     * @param userId 用户ID
     * @return 用户管理视图
     */
    @GetMapping("/{userId}")
    public Result<UserManageVO> detail(@PathVariable("userId") Long userId) {
        UserPO u = userManager.getById(userId);
        if (u == null) {
            throw new BizException(UserCodeEnum.USER_NOT_FOUND);
        }
        return Result.success(toVO(u));
    }

    /**
     * PO → VO 转换。仅暴露非敏感字段，phone 等加密字段不返回。
     * @param u 用户持久化对象
     * @return 用户管理视图
     */
    private UserManageVO toVO(UserPO u) {
        return new UserManageVO(u.getId(), u.getUsername(), u.getNickname(),
                u.getAvatar(), u.getPhoneMasked(), u.getGender(), u.getCreateTime());
    }
}
