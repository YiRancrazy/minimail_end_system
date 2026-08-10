package com.yirancrazy.minimall.platform.service.impl;

import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.platform.constant.PlatformCodeEnum;
import com.yirancrazy.minimall.platform.dto.PlatformUserPageDTO;
import com.yirancrazy.minimall.platform.service.PlatformUserService;
import com.yirancrazy.minimall.platform.vo.PlatformUserVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台用户服务实现；当前阶段契约由 user-service 提供，本端作为契约入口，
 *              返回空分页作为可运行占位，后续接入 Feign 后委托给远端。
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Service
public class PlatformUserServiceImpl implements PlatformUserService {

    /**
     * 分页查询用户列表。占位实现：当前始终返回空游标分页。
     * @param dto 分页入参
     * @return 用户游标分页结果
     */
    @Override
    public CursorPageVO<PlatformUserVO> page(PlatformUserPageDTO dto) {
        int limit = dto.getLimit();
        List<PlatformUserVO> records = Collections.emptyList();
        return CursorPageVO.of(records, limit, u -> u.getId());
    }

    /**
     * 用户详情占位：当前无远端数据可用，固定抛 PLATFORM_USER_NOT_FOUND。
     * @param userId 用户ID
     * @return 用户视图（永不返回，固定抛错）
     * @throws BizException 用户不存在时
     */
    @Override
    public PlatformUserVO detail(Long userId) {
        throw new BizException(PlatformCodeEnum.PLATFORM_USER_NOT_FOUND);
    }
}
