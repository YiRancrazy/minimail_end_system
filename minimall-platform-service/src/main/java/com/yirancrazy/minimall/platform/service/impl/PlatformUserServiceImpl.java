package com.yirancrazy.minimall.platform.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.user.UserManageVO;
import com.yirancrazy.minimall.api.feign.UserFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.constant.PlatformCodeEnum;
import com.yirancrazy.minimall.platform.dto.PlatformUserPageDTO;
import com.yirancrazy.minimall.platform.service.PlatformUserService;
import com.yirancrazy.minimall.platform.vo.PlatformUserVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台用户服务实现，通过 UserFeignClient 委托 user-service；
 *              Feign 不可用时由 FallbackFactory 返回空分页/null。
 * @Version: 1.1
 * @DateTime: 2026/08/10
 **/
@Slf4j
@Service
public class PlatformUserServiceImpl implements PlatformUserService {

    private final UserFeignClient userFeignClient;

    public PlatformUserServiceImpl(UserFeignClient userFeignClient) {
        this.userFeignClient = userFeignClient;
    }

    /**
     * 分页查询用户列表，委托 UserFeignClient.pageManage。
     * @param dto 分页入参
     * @return 用户游标分页结果
     */
    @Override
    public CursorPageVO<PlatformUserVO> page(PlatformUserPageDTO dto) {
        InternalPageQuery q = new InternalPageQuery();
        q.setCursor(dto.getCursor());
        q.setLimit(dto.getLimit());
        q.setKeyword(dto.getKeyword());
        q.setStatus(null);
        Result<CursorPageVO<UserManageVO>> r = userFeignClient.pageManage(q);
        if (r == null || r.getData() == null) {
            log.warn("userFeignClient.pageManage returned empty result");
            return CursorPageVO.of(List.of(),
                    dto.getLimit() == null ? 20 : dto.getLimit(), v -> v.getId());
        }
        return r.getData().map(this::toPlatformVO);
    }

    /**
     * 用户详情，委托 UserFeignClient.detail。
     * @param userId 用户ID
     * @return 用户视图
     * @throws BizException 用户不存在或远端不可用时
     */
    @Override
    public PlatformUserVO detail(Long userId) {
        Result<UserManageVO> r = userFeignClient.detail(userId);
        if (r == null || r.getData() == null) {
            throw new BizException(PlatformCodeEnum.PLATFORM_USER_NOT_FOUND);
        }
        return toPlatformVO(r.getData());
    }

    /**
     * UserManageVO → PlatformUserVO 映射。
     * @param v 跨服务用户视图
     * @return 平台用户视图
     */
    private PlatformUserVO toPlatformVO(UserManageVO v) {
        return new PlatformUserVO(v.getId(), v.getUsername(), v.getNickname(),
                v.getAvatar(), v.getPhoneMasked(), v.getGender(), v.getCreateTime());
    }
}
