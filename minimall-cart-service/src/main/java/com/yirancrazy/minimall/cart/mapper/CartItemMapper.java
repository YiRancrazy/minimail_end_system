package com.yirancrazy.minimall.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.cart.entity.CartItemPo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射购物车管理器使用的 t_cart_item 原子 upsert。SQL 会在唯一键下插入新用户和 SKU
 *     记录，或累加已有数量；最终数量上限为 999，同时递增版本号并刷新更新时间。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface CartItemMapper extends BaseMapper<CartItemPo> {

  /**
   * 插入购物车记录，或向其已有用户和 SKU 记录原子累加数量.
   *
   * <p>勾选值只在插入新记录时写入。命中重复键时仅修改数量、版本号和更新时间，最终数量上限为 999。
   *
   * @param id 新购物车记录使用的主键
   * @param userId 购物车所属用户
   * @param skuId 待插入或合并的 SKU
   * @param quantity 待累加数量
   * @param selected 仅在插入时使用的勾选编码
   * @return MySQL 返回的影响行数
   */
  @Insert(
      """
      INSERT INTO t_cart_item
          (id, user_id, sku_id, quantity, selected, version, is_deleted, create_time, update_time)
      VALUES
          (#{id}, #{userId}, #{skuId}, LEAST(#{quantity}, 999), #{selected}, 0, 0,
           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
      ON DUPLICATE KEY UPDATE
          quantity = LEAST(quantity + #{quantity}, 999),
          version = version + 1,
          update_time = CURRENT_TIMESTAMP
      """)
  int upsertQuantity(
      @Param("id") Long id,
      @Param("userId") Long userId,
      @Param("skuId") Long skuId,
      @Param("quantity") Integer quantity,
      @Param("selected") Integer selected);
}
