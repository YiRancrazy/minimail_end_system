package com.yirancrazy.minimall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.order.entity.OrderItemPo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供 OrderItemPo 的 MyBatis-Plus 基础数据访问能力。
 *   业务边界：只声明数据库查询和条件更新接口，不承载业务规则或跨服务调用。
 *   使用场景：由对应 Manager 注入并调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface OrderItemMapper extends BaseMapper<OrderItemPo> {}
