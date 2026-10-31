package com.yirancrazy.minimall.cart.service;

import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDto;
import com.yirancrazy.minimall.cart.dto.CartItemAddDto;
import com.yirancrazy.minimall.cart.dto.CartUpdateDto;
import com.yirancrazy.minimall.cart.vo.CartItemVo;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义用户购物车管理和订单结算集成所需的购物车域操作。实现类负责用户归属校验、
 *     获取远程商品快照、以原子 upsert 保护并发添加，并在结算清理时应用已结算数量。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface CartService {

  /**
   * 查询一个用户全部持久化购物车记录，并补充商品快照.
   *
   * <p>SKU 或 SPU 快照缺失不会移除购物车记录，对应展示字段保持 null。购物车为空时直接返回空
   * 列表，不发起远程调用。
   *
   * @param userId 购物车所属用户
   * @return 按数据层顺序返回的购物车视图
   */
  List<CartItemVo> listByUser(Long userId);

  /**
   * 向用户购物车添加 SKU 数量并返回持久化记录主键.
   *
   * <p>该操作使用原子插入或累加。数量上限为 999，selected 为空时默认已勾选，勾选值只在首次插入
   * 记录时生效。
   *
   * @param userId 购物车所属用户
   * @param dto 已校验的 SKU 和数量请求
   * @return 新插入或合并后的购物车记录主键
   * @throws com.yirancrazy.minimall.common.exception.BizException upsert 未影响记录或无法读取结果时抛出
   */
  Long add(Long userId, CartItemAddDto dto);

  /**
   * 校验购物车记录归属后删除该记录.
   *
   * @param id 购物车记录主键
   * @param userId 预期所属用户
   * @return 删除影响记录时返回 true
   * @throws com.yirancrazy.minimall.common.exception.BizException 记录不存在或属于其他用户时抛出
   */
  boolean delete(Long id, Long userId);

  /**
   * 统计指定用户拥有的持久化购物车记录数量.
   *
   * @param userId 购物车所属用户
   * @return 记录数量；购物车为空时返回 0
   */
  long countByUser(Long userId);

  /**
   * 对指定用户拥有的购物车记录执行数量或勾选状态的部分更新.
   *
   * <p>请求中的空字段保持原值。勾选值 true 映射为持久化编码 1，false 映射为 0。
   *
   * @param id 购物车记录主键
   * @param userId 预期所属用户
   * @param dto 可选更新字段
   * @return 数据层更新成功时返回 true
   * @throws com.yirancrazy.minimall.common.exception.BizException 记录不存在或属于其他用户时抛出
   */
  boolean update(Long id, Long userId, CartUpdateDto dto);

  /**
   * 将同一勾选编码设置到用户拥有的全部购物车记录.
   *
   * @param userId 购物车所属用户
   * @param selected 持久化勾选编码，预期为 0 或 1
   * @return 批量更新成功时返回 true
   */
  boolean selectAll(Long userId, Integer selected);

  /**
   * 删除用户拥有的全部购物车记录.
   *
   * @param userId 购物车所属用户
   * @return 批量删除成功时返回 true
   */
  boolean clear(Long userId);

  /**
   * 将用户拥有且被指定的购物车记录转换为收藏.
   *
   * <p>所有请求主键都必须解析为用户拥有的记录。实现会先逐 SKU 写入用户服务，再删除本地记录；
   * 远程写入或本地删除失败时购物车记录保留，该方法本身不开启本地事务。
   *
   * @param userId 购物车所属用户
   * @param itemIds 非空购物车记录主键列表
   * @throws com.yirancrazy.minimall.common.exception.BizException 列表为空、记录缺失或越权、
   *     收藏写入失败或本地删除失败时抛出
   */
  void moveToFavorite(Long userId, List<Long> itemIds);

  /**
   * 返回创建订单所需的权威购物车快照.
   *
   * <p>重复主键按首次出现位置去重，响应保持该请求顺序。每个记录都必须属于指定用户，且持久化
   * 勾选编码为 1。
   *
   * @param userId 结算用户
   * @param itemIds 请求的购物车记录主键
   * @return 购物车条目主键、SKU 主键和数量组成的条目
   * @throws com.yirancrazy.minimall.common.exception.BizException 列表为空、主键缺失或越权、
   *     条目未勾选时抛出
   */
  List<CartCheckoutItemDto> getCheckoutItems(Long userId, List<Long> itemIds);

  /**
   * 在订单创建后将已结算数量应用到购物车记录.
   *
   * <p>先校验必填字段和重复购物车主键。SKU 与结算快照不一致时整个操作失败；否则数量大于结算量
   * 的记录执行扣减，数量小于等于结算量的记录直接删除。已缺失记录会跳过，便于此前删除成功后
   * 重试完成剩余清理。
   *
   * @param userId 结算用户
   * @param items 已结算的购物车主键、SKU 主键和数量
   * @throws com.yirancrazy.minimall.common.exception.BizException 入参非法、SKU 不一致或本地
   *     删除、更新失败时抛出
   */
  void cleanupCheckout(Long userId, List<CartCheckoutItemDto> items);
}
