package com.yirancrazy.minimall.id.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.id.entity.IdSegmentPo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: t_id_segment 的 MyBatis-Plus Mapper。仅继承通用 CRUD，没有自定义 SQL；
 *     当前基于 Snowflake 的 IdService 不直接调用该 Mapper。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface IdSegmentMapper extends BaseMapper<IdSegmentPo> {}
