package com.yirancrazy.minimall.merchant.service;

import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDto;
import com.yirancrazy.minimall.merchant.dto.ShopCreateDto;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDto;
import com.yirancrazy.minimall.merchant.entity.ShopPo;
import com.yirancrazy.minimall.merchant.vo.ShopVo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 店铺领域服务契约；定义按商家归属读取、内部快照、游标列表、创建、更新和删除能力，并约束关键业务校验。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface ShopService {

  /**
   * 按店铺主键读取店铺，并可校验商家归属。
   *
   * @param merchantId 可信商家 ID；为 null 时跳过归属校验
   * @param id 店铺主键
   * @return 店铺持久化对象
   * @throws BizException 店铺不存在或 merchantId 与店铺归属不一致时抛出 SHOP_NOT_FOUND
   */
  ShopPo getById(Long merchantId, Long id);

  /**
   * 读取商家自有店铺并转换为展示视图。
   *
   * @param merchantId 可信商家 ID
   * @param id 店铺主键
   * @return 店铺视图
   * @throws BizException 店铺不存在或不属于该商家时抛出 SHOP_NOT_FOUND
   */
  ShopVo getVo(Long merchantId, Long id);

  /**
   * 读取服务间调用所需的店铺快照，不执行商家归属校验。
   *
   * 未知状态码会映射为 null alias，调用方需结合 merchantId 自行判断归属。
   *
   * @param id 店铺主键
   * @return 店铺 ID、商家 ID、名称和状态别名快照
   * @throws BizException 店铺不存在时抛出 SHOP_NOT_FOUND
   */
  ShopSnapshotDto getSnapshot(Long id);

  /**
   * 按商家 ID 游标查询店铺，结果按 ID 倒序返回。
   *
   * limit 为空或非正数时使用 20，最大限制为 100；非数字 cursor 会被忽略。
   *
   * @param merchantId 可信商家 ID
   * @param cursor 上一页末条店铺 ID
   * @param limit 期望返回条数
   * @return 当前页店铺持久化对象列表
   */
  java.util.List<ShopPo> list(Long merchantId, String cursor, Integer limit);

  /**
   * 按商家 ID 游标查询店铺并转换为展示视图。
   *
   * @param merchantId 可信商家 ID
   * @param cursor 上一页末条店铺 ID
   * @param limit 期望返回条数
   * @return 当前页店铺视图列表
   */
  java.util.List<ShopVo> listVo(Long merchantId, String cursor, Integer limit);

  /**
   * 为已审核通过的商家创建营业中店铺。
   *
   * @param merchantId 可信商家身份，对应商家表的 userId
   * @param dto 已校验的店铺名称和证照号
   * @return 新店铺主键
   * @throws BizException 商家不存在或资质未审核通过时抛出 SHOP_MERCHANT_NOT_APPROVED
   */
  Long create(Long merchantId, ShopCreateDto dto);

  /**
   * 更新商家自有店铺的名称、证照号和可选状态。
   *
   * status 为空时保持原状态；商家不能将店铺设置为 SUSPENDED。
   *
   * @param merchantId 可信商家 ID
   * @param id 店铺主键
   * @param dto 已校验的更新内容
   * @return MyBatis-Plus 更新操作是否成功
   * @throws BizException 店铺不存在、归属不符、设置冻结状态或状态别名非法时抛出
   */
  boolean update(Long merchantId, Long id, ShopUpdateDto dto);

  /**
   * 逻辑删除商家自有店铺。
   *
   * @param merchantId 可信商家 ID
   * @param id 店铺主键
   * @return MyBatis-Plus 删除操作是否成功
   * @throws BizException 店铺不存在或不属于该商家时抛出 SHOP_NOT_FOUND
   */
  boolean delete(Long merchantId, Long id);
}
