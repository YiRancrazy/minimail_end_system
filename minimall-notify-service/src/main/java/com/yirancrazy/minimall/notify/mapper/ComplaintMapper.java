package com.yirancrazy.minimall.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.notify.entity.ComplaintPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供 t_notify_complaint 投诉记录的基础 MyBatis-Plus 持久化能力，不定义额外 SQL.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Mapper
public interface ComplaintMapper extends BaseMapper<ComplaintPo> {}
