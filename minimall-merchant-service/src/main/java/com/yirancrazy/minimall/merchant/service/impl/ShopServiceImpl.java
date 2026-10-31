package com.yirancrazy.minimall.merchant.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.merchant.constant.MerchantAuditStatusEnum;
import com.yirancrazy.minimall.merchant.constant.ShopCodeEnum;
import com.yirancrazy.minimall.merchant.constant.ShopStatusEnum;
import com.yirancrazy.minimall.merchant.dto.ShopCreateDto;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDto;
import com.yirancrazy.minimall.merchant.entity.MerchantPo;
import com.yirancrazy.minimall.merchant.entity.ShopPo;
import com.yirancrazy.minimall.merchant.manager.MerchantManager;
import com.yirancrazy.minimall.merchant.manager.ShopManager;
import com.yirancrazy.minimall.merchant.service.ShopService;
import com.yirancrazy.minimall.merchant.vo.ShopVo;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 店铺服务实现；依赖 ShopManager 与 MerchantManager，校验店铺归属和商家审核状态，并处理状态转换、内部快照及逻辑删除。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Service
public class ShopServiceImpl implements ShopService {

  private final ShopManager shopManager;
  private final MerchantManager merchantManager;

  /**
   * 注入店铺与商家资质持久化依赖。
   *
   * @param shopManager 店铺 Manager
   * @param merchantManager 商家资质 Manager
   */
  public ShopServiceImpl(ShopManager shopManager, MerchantManager merchantManager) {
    this.shopManager = shopManager;
    this.merchantManager = merchantManager;
  }

  /**
   * 读取店铺并在存在商家上下文时校验归属。
   *
   * merchantId 为 null 表示内部调用跳过归属校验；店铺不存在或归属不一致统一按店铺不存在处理。
   *
   * @param merchantId 可信商家 ID，可为 null
   * @param id 店铺主键
   * @return 店铺持久化对象
   * @throws BizException 店铺不存在或归属不一致时抛出 SHOP_NOT_FOUND
   */
  public ShopPo getById(Long merchantId, Long id) {
    ShopPo s = shopManager.getById(id);
    if (s == null || (merchantId != null && !merchantId.equals(s.getMerchantId()))) {
      throw new BizException(ShopCodeEnum.SHOP_NOT_FOUND);
    }
    return s;
  }

  /**
   * 读取商家自有店铺并映射为展示视图。
   *
   * @param merchantId 可信商家 ID
   * @param id 店铺主键
   * @return 店铺视图
   * @throws BizException 店铺不存在或不属于该商家时抛出 SHOP_NOT_FOUND
   */
  @Override
  public ShopVo getVo(Long merchantId, Long id) {
    return ShopVo.from(getById(merchantId, id));
  }

  /**
   * 构建服务间调用所需的店铺快照。
   *
   * 内部调用不校验商家归属；店铺状态通过 ShopStatusEnum 转为 alias，未知状态码返回 null alias。
   *
   * @param id 店铺主键
   * @return 店铺快照
   * @throws BizException 店铺不存在时抛出 SHOP_NOT_FOUND
   */
  @Override
  public ShopSnapshotDto getSnapshot(Long id) {
    // 内部服务间调用没有商家上下文，跳过归属校验，由调用方使用 merchantId 判断归属
    ShopPo shop = getById(null, id);
    ShopStatusEnum status = ShopStatusEnum.fromCode(shop.getStatus());
    return new ShopSnapshotDto(
        shop.getId(),
        shop.getMerchantId(),
        shop.getShopName(),
        status == null ? null : status.getAlias());
  }

  /**
   * 按商家 ID 游标查询店铺并按 ID 倒序返回。
   *
   * limit 为空或非正数时使用 20，最大 100；cursor 非数字时忽略游标条件。
   *
   * @param merchantId 可信商家 ID
   * @param cursor 上一页末条店铺 ID
   * @param limit 期望返回条数
   * @return 当前页店铺持久化对象列表
   */
  public java.util.List<ShopPo> list(Long merchantId, String cursor, Integer limit) {
    int safeLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
    com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ShopPo> q =
        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
    q.eq("merchant_id", merchantId);
    if (cursor != null && !cursor.isBlank()) {
      try {
        q.lt("id", Long.parseLong(cursor));
      } catch (NumberFormatException ignored) {
        // cursor 非数字时按忽略处理，依赖兜底分页
      }
    }
    q.orderByDesc("id").last("LIMIT " + safeLimit);
    return shopManager.list(q);
  }

  /**
   * 查询当前商家店铺并逐条转换为展示视图。
   *
   * @param merchantId 可信商家 ID
   * @param cursor 上一页末条店铺 ID
   * @param limit 期望返回条数
   * @return 当前页店铺视图列表
   */
  @Override
  public java.util.List<ShopVo> listVo(Long merchantId, String cursor, Integer limit) {
    return list(merchantId, cursor, limit).stream().map(ShopVo::from).toList();
  }

  /**
   * 校验商家资质后创建营业中店铺。
   *
   * 商家记录不存在或审核状态不是 APPROVED 时拒绝创建；保存成功后返回 MyBatis-Plus 回填的店铺主键。
   *
   * @param merchantId 商家用户 ID，对应商家表 user_id
   * @param dto 店铺名称与证照号
   * @return 新店铺主键
   * @throws BizException 商家不存在或资质未审核通过时抛出 SHOP_MERCHANT_NOT_APPROVED
   */
  @Override
  public Long create(Long merchantId, ShopCreateDto dto) {
    MerchantPo merchant =
        merchantManager.getOne(
            Wrappers.lambdaQuery(MerchantPo.class).eq(MerchantPo::getUserId, merchantId));
    if (merchant == null
        || merchant.getAuditStatus() == null
        || merchant.getAuditStatus()
            != Integer.parseInt(MerchantAuditStatusEnum.APPROVED.getCode())) {
      throw new BizException(ShopCodeEnum.SHOP_MERCHANT_NOT_APPROVED);
    }
    ShopPo shop = new ShopPo();
    shop.setMerchantId(merchantId);
    shop.setShopName(dto.getShopName());
    shop.setLicenseNo(dto.getLicenseNo());
    shop.setStatus(ShopStatusEnum.ACTIVE.intCode());
    shopManager.save(shop);
    return shop.getId();
  }

  /**
   * 更新商家自有店铺并处理可选状态变更。
   *
   * 先执行归属校验；SUSPENDED 禁止商家设置，status 为空时只更新名称与证照号，非空时按 alias 转成持久化 code。
   *
   * @param merchantId 可信商家 ID
   * @param id 店铺主键
   * @param dto 店铺更新内容
   * @return updateById 是否影响记录
   * @throws BizException 店铺归属校验失败、设置冻结状态或状态别名非法时抛出
   */
  @Override
  public boolean update(Long merchantId, Long id, ShopUpdateDto dto) {
    getById(merchantId, id);
    if (ShopStatusEnum.SUSPENDED.getAlias().equals(dto.getStatus())) {
      throw new BizException(ShopCodeEnum.SHOP_STATUS_FORBIDDEN);
    }
    ShopPo shop = new ShopPo();
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
   * 校验归属后逻辑删除店铺。
   *
   * BasePo 的 @TableLogic 配置会把删除操作转换为逻辑删除。
   *
   * @param merchantId 可信商家 ID
   * @param id 店铺主键
   * @return removeById 是否影响记录
   * @throws BizException 店铺不存在或不属于该商家时抛出 SHOP_NOT_FOUND
   */
  @Override
  public boolean delete(Long merchantId, Long id) {
    getById(merchantId, id);
    return shopManager.removeById(id);
  }

  /**
   * 将店铺状态 alias 转换为持久化整数 code。
   *
   * @param status 对外状态别名，如 ACTIVE、INACTIVE
   * @return 对应状态整数 code
   * @throws BizException alias 无法匹配 ShopStatusEnum 时抛出 SHOP_STATUS_INVALID
   */
  private int resolveStatus(String status) {
    ShopStatusEnum statusEnum = ShopStatusEnum.fromAlias(status);
    if (statusEnum == null) {
      throw new BizException(ShopCodeEnum.SHOP_STATUS_INVALID);
    }
    return statusEnum.intCode();
  }
}
