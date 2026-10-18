package com.yirancrazy.minimall.platform.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.user.UserManageVO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.platform.constant.PlatformCodeEnum;
import com.yirancrazy.minimall.platform.dto.PlatformUserPageDTO;
import com.yirancrazy.minimall.platform.gateway.PlatformRemoteGateway;
import com.yirancrazy.minimall.platform.service.PlatformUserService;
import com.yirancrazy.minimall.platform.vo.PlatformUserVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformUserServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
public class PlatformUserServiceImpl implements PlatformUserService {

    private final PlatformRemoteGateway platformRemoteGateway;
    private final MinioUtil minioUtil;

    /**
     * 构造 PlatformUserServiceImpl 实例。
     * @param platformRemoteGateway platformRemoteGateway 参数
     * @param minioUtil minioUtil 参数
     */
    public PlatformUserServiceImpl(PlatformRemoteGateway platformRemoteGateway, MinioUtil minioUtil) {
        this.platformRemoteGateway = platformRemoteGateway;
        this.minioUtil = minioUtil;
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<PlatformUserVO> page(PlatformUserPageDTO dto) {
        InternalPageQuery q = new InternalPageQuery();
        q.setCursor(dto.getCursor());
        q.setLimit(dto.getLimit());
        q.setKeyword(dto.getKeyword());
        q.setStatus(null);
        Result<CursorPageVO<UserManageVO>> r = platformRemoteGateway.pageUsers(q);
        if (r == null || r.getData() == null) {
            log.warn("用户分页查询返回空结果");
            return CursorPageVO.of(List.of(),
                    dto.getLimit() == null ? 20 : dto.getLimit(), v -> v.getId());
        }
        return r.getData().map(this::toPlatformVO);
    }


    /**
     * 执行 detail 方法。
     * @param userId userId 参数
     * @return 处理结果
     */
    @Override
    public PlatformUserVO detail(Long userId) {
        Result<UserManageVO> r = platformRemoteGateway.userDetail(userId);
        if (r == null || r.getData() == null) {
            throw new BizException(PlatformCodeEnum.PLATFORM_USER_NOT_FOUND);
        }
        return toPlatformVO(r.getData());
    }


    /**
     * 转换或解析平台VO。
     * @param v v
     * @return 视图对象
     */
    private PlatformUserVO toPlatformVO(UserManageVO v) {
        String avatarUrl = minioUtil.resolvePublicUrl(v.getAvatar());
        return new PlatformUserVO(v.getId(), v.getUsername(), v.getNickname(),
                avatarUrl, v.getPhoneMasked(), v.getGender(), v.getCreateTime());
    }
}
