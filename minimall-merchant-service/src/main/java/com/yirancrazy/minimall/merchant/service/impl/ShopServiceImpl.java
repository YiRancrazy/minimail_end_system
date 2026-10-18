package com.yirancrazy.minimall.merchant.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.merchant.constant.MerchantAuditStatusEnum;
import com.yirancrazy.minimall.merchant.constant.ShopCodeEnum;
import com.yirancrazy.minimall.merchant.constant.ShopStatusEnum;
import com.yirancrazy.minimall.merchant.dto.ShopCreateDTO;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDTO;
import com.yirancrazy.minimall.merchant.entity.MerchantPO;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.manager.MerchantManager;
import com.yirancrazy.minimall.merchant.manager.ShopManager;
import com.yirancrazy.minimall.merchant.service.ShopService;
import com.yirancrazy.minimall.merchant.vo.ShopVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Service
public class ShopServiceImpl implements ShopService {

    private final ShopManager shopManager;
    private final MerchantManager merchantManager;

    /**
     * 构造 ShopServiceImpl 实例。
     * @param shopManager shopManager 参数
     * @param merchantManager merchantManager 参数
     */
    public ShopServiceImpl(ShopManager shopManager, MerchantManager merchantManager) {
        this.shopManager = shopManager;
        this.merchantManager = merchantManager;
    }


    /**
     * 获取相关数据。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @return 处理结果
     */
    public ShopPO getById(Long merchantId, Long id) {
        ShopPO s = shopManager.getById(id);
        if (s == null || (merchantId != null && !merchantId.equals(s.getMerchantId()))) {
            throw new BizException(ShopCodeEnum.SHOP_NOT_FOUND);
        }
        return s;
    }


    /**
     * 获取相关数据。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @return 处理结果
     */
    @Override
    public ShopVO getVO(Long merchantId, Long id) {
        return ShopVO.from(getById(merchantId, id));
    }


    /**
     * 获取相关数据。
     * @param id id 参数
     * @return 处理结果
     */
    @Override
    public ShopSnapshotDTO getSnapshot(Long id) {
        // 内部服务间调用没有商家上下文，跳过归属校验，由调用方使用 merchantId 判断归属
        ShopPO shop = getById(null, id);
        ShopStatusEnum status = ShopStatusEnum.fromCode(shop.getStatus());
        return new ShopSnapshotDTO(shop.getId(), shop.getMerchantId(), shop.getShopName(),
            status == null ? null : status.getAlias());
    }


    /**
     * 查询列表数据。
     * @param merchantId merchantId 参数
     * @param cursor cursor 参数
     * @param limit limit 参数
     * @return 处理结果
     */
    public java.util.List<ShopPO> list(Long merchantId, String cursor, Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ShopPO> q =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        q.eq("merchant_id", merchantId);
        if (cursor != null && !cursor.isBlank()) {
            try {
                q.lt("id", Long.parseLong(cursor));
            }
            catch (NumberFormatException ignored) {
                // cursor 非数字时按忽略处理，依赖兜底分页
            }
        }
        q.orderByDesc("id").last("LIMIT " + safeLimit);
        return shopManager.list(q);
    }


    /**
     * 查询列表数据。
     * @param merchantId merchantId 参数
     * @param cursor cursor 参数
     * @param limit limit 参数
     * @return 处理结果
     */
    @Override
    public java.util.List<ShopVO> listVO(Long merchantId, String cursor, Integer limit) {
        return list(merchantId, cursor, limit).stream().map(ShopVO::from).toList();
    }


    /**
     * 创建业务数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 业务数值
     */
    @Override
    public Long create(Long merchantId, ShopCreateDTO dto) {
        MerchantPO merchant = merchantManager.getOne(
            Wrappers.lambdaQuery(MerchantPO.class).eq(MerchantPO::getUserId, merchantId));
        if (merchant == null || merchant.getAuditStatus() == null
            || merchant.getAuditStatus() != Integer.parseInt(MerchantAuditStatusEnum.APPROVED.getCode())) {
            throw new BizException(ShopCodeEnum.SHOP_MERCHANT_NOT_APPROVED);
        }
        ShopPO shop = new ShopPO();
        shop.setMerchantId(merchantId);
        shop.setShopName(dto.getShopName());
        shop.setLicenseNo(dto.getLicenseNo());
        shop.setStatus(ShopStatusEnum.ACTIVE.intCode());
        shopManager.save(shop);
        return shop.getId();
    }


    /**
     * 更新业务数据。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @param dto dto 参数
     * @return 是否处理成功
     */
    @Override
    public boolean update(Long merchantId, Long id, ShopUpdateDTO dto) {
        getById(merchantId, id);
        if (ShopStatusEnum.SUSPENDED.getAlias().equals(dto.getStatus())) {
            throw new BizException(ShopCodeEnum.SHOP_STATUS_FORBIDDEN);
        }
        ShopPO shop = new ShopPO();
        shop.setId(id);
        shop.setShopName(dto.getShopName());
        shop.setLicenseNo(dto.getLicenseNo());
        // 状态未传视为不修改，保持服务端既有管控值
        if (dto.getStatus() != null) {
            shop.setStatus(resolveStatus(dto.getStatus()));
        }
        return shopManager.updateById(shop);
    }


    /**
     * 删除业务数据。
     * @param merchantId merchantId 参数
     * @param id id 参数
     * @return 是否处理成功
     */
    @Override
    public boolean delete(Long merchantId, Long id) {
        getById(merchantId, id);
        return shopManager.removeById(id);
    }


    /**
     * 转换或解析状态。
     * @param status 状态
     * @return 处理结果
     * @throws BizException 业务校验失败时抛出
     */
    private int resolveStatus(String status) {
        ShopStatusEnum statusEnum = ShopStatusEnum.fromAlias(status);
        if (statusEnum == null) {
            throw new BizException(ShopCodeEnum.SHOP_STATUS_INVALID);
        }
        return statusEnum.intCode();
    }
}
