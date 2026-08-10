package com.yirancrazy.minimall.notify.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.notify.entity.CommentPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品评价 MyBatis Mapper 接口，映射 t_notify_comment 表
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Mapper
public interface CommentMapper extends BaseMapper<CommentPO> {
}
