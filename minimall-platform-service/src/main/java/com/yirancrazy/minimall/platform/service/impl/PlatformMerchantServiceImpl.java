package com.yirancrazy.minimall.platform.service.impl;

import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.platform.constant.PlatformCodeEnum;
import com.yirancrazy.minimall.platform.dto.PlatformMerchantPageDTO;
import com.yirancrazy.minimall.platform.service.PlatformMerchantService;
import com.yirancrazy.minimall.platform.vo.PlatformMerchantVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台商家服务实现；当前阶段契约由 merchant-service 提供，本端作为契约入口，
 *              返回空分页作为可运行占位，后续接入 Feign 后委托给远端。
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Service
public class PlatformMerchantServiceImpl implements PlatformMerchantService {

    /**
     * 分页查询商家列表。占位实现：当前始终返回空游标分页。
     * @param dto 分页入参
     * @return 商家游标分页结果
     */
    @Override
    public CursorPageVO<PlatformMerchantVO> page(PlatformMerchantPageDTO dto) {
        int limit = dto.getLimit();
        List<PlatformMerchantVO> records = Collections.emptyList();
        return CursorPageVO.of(records, limit, m -> m.getMerchantId());
    }

    /**
     * 商家详情占位：当前无远端数据可用，固定抛 PLATFORM_MERCHANT_NOT_FOUND。
     * @param merchantId 商家ID
     * @return 商家视图（永不返回，固定抛错）
     * @throws BizException 商家不存在时
     */
    @Override
    public PlatformMerchantVO detail(Long merchantId) {
        throw new BizException(PlatformCodeEnum.PLATFORM_MERCHANT_NOT_FOUND);
    }
}
