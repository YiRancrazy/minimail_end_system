package com.yirancrazy.minimall.platform.service.impl;

import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.user.UserManageVo;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.platform.constant.PlatformCodeEnum;
import com.yirancrazy.minimall.platform.dto.PlatformUserPageDto;
import com.yirancrazy.minimall.platform.gateway.PlatformRemoteGateway;
import com.yirancrazy.minimall.platform.service.PlatformUserService;
import com.yirancrazy.minimall.platform.vo.PlatformUserVo;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台用户管理实现，通过远程用户服务查询数据，并解析头像公开访问地址.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
public class PlatformUserServiceImpl implements PlatformUserService {

    private final PlatformRemoteGateway platformRemoteGateway;
    private final MinioUtil minioUtil;

    /**
     * 注入远程平台访问网关和 MinIO 地址解析工具.
     *
     * @param platformRemoteGateway 平台远程服务访问网关
     * @param minioUtil MinIO 公开地址解析工具
     */
    public PlatformUserServiceImpl(PlatformRemoteGateway platformRemoteGateway, MinioUtil minioUtil) {
        this.platformRemoteGateway = platformRemoteGateway;
        this.minioUtil = minioUtil;
    }

    /**
     * 将用户分页条件转换为内部查询，并映射用户服务返回结果.
     *
     * <p>远程响应或数据为空时返回空分页；未指定 limit 时使用 20，用户状态筛选固定为空。
     *
     * @param dto 用户分页筛选条件
     * @return 用户视图分页
     */
    @Override
    public CursorPageVo<PlatformUserVo> page(PlatformUserPageDto dto) {
        InternalPageQuery q = new InternalPageQuery();
        q.setCursor(dto.getCursor());
        q.setLimit(dto.getLimit());
        q.setKeyword(dto.getKeyword());
        q.setStatus(null);
        Result<CursorPageVo<UserManageVo>> r = platformRemoteGateway.pageUsers(q);
        if (r == null || r.getData() == null) {
            log.warn("用户分页查询返回空结果");
            return CursorPageVo.of(
                    List.of(), dto.getLimit() == null ? 20 : dto.getLimit(), v -> v.getId());
        }
        return r.getData().map(this::toPlatformVo);
    }

    /**
     * 查询用户详情并转换为平台管理视图.
     *
     * @param userId 用户 ID
     * @return 用户管理视图
     * @throws BizException 用户服务响应为空或用户不存在时抛出
     */
    @Override
    public PlatformUserVo detail(Long userId) {
        Result<UserManageVo> r = platformRemoteGateway.userDetail(userId);
        if (r == null || r.getData() == null) {
            throw new BizException(PlatformCodeEnum.PLATFORM_USER_NOT_FOUND);
        }
        return toPlatformVo(r.getData());
    }

    /**
     * 将用户服务数据转换为平台视图，并解析头像公开访问地址.
     *
     * @param v 用户服务返回的管理数据
     * @return 平台用户视图
     */
    private PlatformUserVo toPlatformVo(UserManageVo v) {
        String avatarUrl = minioUtil.resolvePublicUrl(v.getAvatar());
        return new PlatformUserVo(
                v.getId(),
                v.getNickname(),
                avatarUrl,
                v.getPhoneMasked(),
                v.getGender(),
                v.getCreateTime());
    }
}
