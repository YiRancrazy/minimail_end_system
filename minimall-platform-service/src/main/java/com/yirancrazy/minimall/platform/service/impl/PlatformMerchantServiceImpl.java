package com.yirancrazy.minimall.platform.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.platform.constant.PlatformCodeEnum;
import com.yirancrazy.minimall.platform.dto.MerchantQualificationAuditDTO;
import com.yirancrazy.minimall.platform.dto.PlatformMerchantPageDTO;
import com.yirancrazy.minimall.platform.gateway.PlatformRemoteGateway;
import com.yirancrazy.minimall.platform.service.PlatformMerchantService;
import com.yirancrazy.minimall.platform.vo.MerchantQualificationVO;
import com.yirancrazy.minimall.platform.vo.PlatformMerchantVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformMerchantServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
public class PlatformMerchantServiceImpl implements PlatformMerchantService {

    private final PlatformRemoteGateway platformRemoteGateway;
    private final MinioUtil minioUtil;

    /**
     * 构造 PlatformMerchantServiceImpl 实例。
     * @param platformRemoteGateway platformRemoteGateway 参数
     * @param minioUtil minioUtil 参数
     */
    public PlatformMerchantServiceImpl(PlatformRemoteGateway platformRemoteGateway, MinioUtil minioUtil) {
        this.platformRemoteGateway = platformRemoteGateway;
        this.minioUtil = minioUtil;
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<PlatformMerchantVO> page(PlatformMerchantPageDTO dto) {
        InternalPageQuery q = new InternalPageQuery();
        q.setCursor(dto.getCursor());
        q.setLimit(dto.getLimit());
        q.setKeyword(dto.getKeyword());
        q.setStatus(dto.getAuditStatus());
        Result<CursorPageVO<MerchantManageVO>> r = platformRemoteGateway.pageMerchants(q);
        if (r == null || r.getData() == null) {
            log.warn("商家分页查询返回空结果");
            return CursorPageVO.of(List.of(),
                    dto.getLimit() == null ? 20 : dto.getLimit(), v -> v.getMerchantId());
        }
        return r.getData().map(this::toPlatformVO);
    }


    /**
     * 执行 detail 方法。
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    @Override
    public PlatformMerchantVO detail(Long merchantId) {
        Result<MerchantManageVO> r = platformRemoteGateway.merchantDetail(merchantId);
        if (r == null || r.getData() == null) {
            throw new BizException(PlatformCodeEnum.PLATFORM_MERCHANT_NOT_FOUND);
        }
        return toPlatformVO(r.getData());
    }


    /**
     * 查询列表数据。
     * @param cursor cursor 参数
     * @param limit limit 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<MerchantQualificationVO> listPendingQualifications(String cursor, int limit) {
        InternalPageQuery query = new InternalPageQuery();
        query.setCursor(cursor);
        query.setLimit(limit);
        // 商家资质待审核码：MerchantAuditStatusEnum.PENDING.code = 0
        query.setStatus(0);
        Result<CursorPageVO<MerchantManageVO>> result = platformRemoteGateway.pageMerchants(query);
        if (result == null || !CommonCode.SUCCESS.equals(result.getCode()) || result.getData() == null) {
            log.warn("查询待审核商家资质失败，cursor={}，limit={}", cursor, limit);
            throw new BizException(CommonCode.SYS_ERROR, "商家服务处理失败");
        }
        return result.getData().map(this::toQualificationVO);
    }


    /**
     * 执行 auditQualification 方法。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     */
    public void auditQualification(Long merchantId, MerchantQualificationAuditDTO dto) {
        log.info("平台审核商家资质，merchantId={}，qualificationId={}，approved={}，reason={}",
                merchantId, dto.getQualificationId(), dto.getApproved(), dto.getReason());
        Result<Void> result = platformRemoteGateway.auditMerchant(
            merchantId, dto.getApproved(), dto.getReason());
        if (result == null || !CommonCode.SUCCESS.equals(result.getCode())) {
            log.warn("merchant-service 拒绝商家审核，merchantId={}，result={}", merchantId, result);
            throw new BizException(CommonCode.SYS_ERROR, "商家服务处理失败");
        }
    }


    /**
     * 转换或解析平台VO。
     * @param v v
     * @return 视图对象
     */
    private PlatformMerchantVO toPlatformVO(MerchantManageVO v) {
        return new PlatformMerchantVO(v.getMerchantId(), v.getUserId(),
                v.getMerchantName(), v.getLicenseNo(), v.getAuditStatus(),
                v.getAuditReason(), v.getAuditAt(), v.getCreateTime());
    }


    /**
     * 转换或解析资质VO。
     * @param manage manage
     * @return 视图对象
     */
    private MerchantQualificationVO toQualificationVO(MerchantManageVO manage) {
        MerchantQualificationVO vo = new MerchantQualificationVO();
        vo.setId(manage.getMerchantId());
        vo.setMerchantId(manage.getMerchantId());
        vo.setMerchantName(manage.getMerchantName());
        vo.setLicenseNo(manage.getLicenseNo());
        vo.setLicenseImageUrl(minioUtil.resolvePublicUrl(manage.getLicenseImageUrl()));
        vo.setStatus(auditStatusName(manage.getAuditStatus()));
        vo.setRejectReason(manage.getAuditReason());
        vo.setSubmitTime(manage.getCreateTime());
        vo.setAuditTime(manage.getAuditAt());
        return vo;
    }


    /**
     * 处理审核状态Name。
     * @param code 编码
     * @return 处理结果
     */
    private String auditStatusName(Integer code) {
        if (code == null) {
            return "PENDING";
        }
        switch (code) {
            case 0:
                return "PENDING";
            case 1:
                return "APPROVED";
            case 2:
                return "REJECTED";
            default:
                return String.valueOf(code);
        }
    }
}
