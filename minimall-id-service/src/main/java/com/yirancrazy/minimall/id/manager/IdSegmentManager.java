package com.yirancrazy.minimall.id.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.id.entity.IdSegmentPo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: t_id_segment 持久化的 MyBatis-Plus IService 契约。暴露继承的 CRUD 和批量
 *     操作，但当前基于 Snowflake 的 IdService 未使用该管理器。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface IdSegmentManager extends IService<IdSegmentPo> {}
