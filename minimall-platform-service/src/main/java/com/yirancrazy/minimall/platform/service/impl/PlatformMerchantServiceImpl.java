package com.yirancrazy.minimall.platform.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO;
import com.yirancrazy.minimall.api.feign.MerchantFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.constant.PlatformCodeEnum;
import com.yirancrazy.minimall.platform.dto.PlatformMerchantPageDTO;
import com.yirancrazy.minimall.platform.service.PlatformMerchantService;
import com.yirancrazy.minimall.platform.vo.PlatformMerchantVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台商家服务实现，通过 MerchantFeignClient 委托 merchant-service；
 *              Feign 不可用时由 FallbackFactory 返回空分页/null。
 * @Version: 1.1
 * @DateTime: 2026/08/10
 **/
@Slf4j
@Service
public class PlatformMerchantServiceImpl implements PlatformMerchantService {

    private final MerchantFeignClient merchantFeignClient;

    public PlatformMerchantServiceImpl(MerchantFeignClient merchantFeignClient) {
        this.merchantFeignClient = merchantFeignClient;
    }

    /**
     * 分页查询商家列表，委托 MerchantFeignClient.pageManage。
     * @param dto 分页入参
     * @return 商家游标分页结果
     */
    @Override
    public CursorPageVO<PlatformMerchantVO> page(PlatformMerchantPageDTO dto) {
        InternalPageQuery q = new InternalPageQuery();
        q.setCursor(dto.getCursor());
        q.setLimit(dto.getLimit());
        q.setKeyword(dto.getKeyword());
        q.setStatus(dto.getAuditStatus());
        Result<CursorPageVO<MerchantManageVO>> r = merchantFeignClient.pageManage(q);
        if (r == null || r.getData() == null) {
            log.warn("merchantFeignClient.pageManage returned empty result");
            return CursorPageVO.of(List.of(),
                    dto.getLimit() == null ? 20 : dto.getLimit(), v -> v.getMerchantId());
        }
        return r.getData().map(this::toPlatformVO);
    }

    /**
     * 商家详情，委托 MerchantFeignClient.detail。
     * @param merchantId 商家ID
     * @return 商家视图
     * @throws BizException 商家不存在或远端不可用时
     */
    @Override
    public PlatformMerchantVO detail(Long merchantId) {
        Result<MerchantManageVO> r = merchantFeignClient.detail(merchantId);
        if (r == null || r.getData() == null) {
            throw new BizException(PlatformCodeEnum.PLATFORM_MERCHANT_NOT_FOUND);
        }
        return toPlatformVO(r.getData());
    }

    /**
     * MerchantManageVO → PlatformMerchantVO 映射。
     * @param v 跨服务商家视图
     * @return 平台商家视图
     */
    private PlatformMerchantVO toPlatformVO(MerchantManageVO v) {
        return new PlatformMerchantVO(v.getMerchantId(), v.getUserId(),
                v.getMerchantName(), v.getLicenseNo(), v.getAuditStatus(),
                v.getAuditReason(), v.getAuditAt(), v.getCreateTime());
    }
}
