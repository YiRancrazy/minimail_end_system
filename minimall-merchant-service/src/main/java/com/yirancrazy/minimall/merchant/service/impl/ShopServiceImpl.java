package com.yirancrazy.minimall.merchant.service.impl;

import org.springframework.stereotype.Service;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.merchant.constant.ShopCodeEnum;
import com.yirancrazy.minimall.merchant.constant.ShopStatusEnum;
import com.yirancrazy.minimall.merchant.dto.ShopCreateDTO;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDTO;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.manager.ShopManager;
import com.yirancrazy.minimall.merchant.service.ShopService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商户领域服务实现，实现Shop相关业务逻辑
 * @Version: 1.1
 * @DateTime: 2026/08/16
 */
@Service
public class ShopServiceImpl implements ShopService {

    private final ShopManager shopManager;

    public ShopServiceImpl(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    /**
     * 根据主键 ID 查询店铺并校验归属：非本人名下店铺对外表现为"不存在"，避免泄露资源存在性。
     *
     * @param merchantId 商家账号 ID；为空表示内部服务间调用，跳过归属校验
     * @param id 店铺主键 ID
     * @return 店铺实体对象
     */
    @Override
    public ShopPO getById(Long merchantId, Long id) {
        ShopPO s = shopManager.getById(id);
        if (s == null || (merchantId != null && !merchantId.equals(s.getMerchantId()))) {
            throw new BizException(ShopCodeEnum.SHOP_NOT_FOUND);
        }
        return s;
    }

    /**
     * 分页查询某商家名下的店铺列表，空游标或非法游标降级为首页。
     * merchantId 为必传归属条件，防止越权读取其他商家店铺。
     */
    @Override
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
     * 新增店铺记录并绑定归属商家，返回持久化后的主键 ID。
     *
     * @param merchantId 商家账号 ID
     * @param dto 待创建的店铺信息
     * @return 新建店铺的主键 ID
     */
    @Override
    public Long create(Long merchantId, ShopCreateDTO dto) {
        ShopPO shop = new ShopPO();
        shop.setMerchantId(merchantId);
        shop.setShopName(dto.getShopName());
        shop.setLicenseNo(dto.getLicenseNo());
        shop.setStatus(resolveStatus(dto.getStatus()));
        shopManager.save(shop);
        return shop.getId();
    }

    /**
     * 根据主键 ID 更新店铺信息，先校验归属（复用 getById 的越权判定）。
     *
     * @param merchantId 商家账号 ID
     * @param id 店铺主键 ID
     * @param dto 待更新的店铺信息
     * @return 是否更新成功
     */
    @Override
    public boolean update(Long merchantId, Long id, ShopUpdateDTO dto) {
        getById(merchantId, id);
        ShopPO shop = new ShopPO();
        shop.setId(id);
        shop.setShopName(dto.getShopName());
        shop.setLicenseNo(dto.getLicenseNo());
        shop.setStatus(resolveStatus(dto.getStatus()));
        return shopManager.updateById(shop);
    }

    /**
     * 根据主键 ID 逻辑删除店铺记录，先校验归属（复用 getById 的越权判定）。
     *
     * @param merchantId 商家账号 ID
     * @param id 店铺主键 ID
     * @return 是否删除成功
     */
    @Override
    public boolean delete(Long merchantId, Long id) {
        getById(merchantId, id);
        return shopManager.removeById(id);
    }

    /**
     * 将入参状态字符串转换为 ShopStatusEnum int code，未知状态抛参数错误。
     * @param status DTO 传入的状态字符串（ACTIVE/INACTIVE/SUSPENDED）
     * @return 持久化状态码
     */
    private int resolveStatus(String status) {
        ShopStatusEnum statusEnum = ShopStatusEnum.fromAlias(status);
        if (statusEnum == null) {
            throw new BizException(ShopCodeEnum.SHOP_STATUS_INVALID);
        }
        return statusEnum.intCode();
    }
}