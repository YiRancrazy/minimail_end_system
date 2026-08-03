package com.yirancrazy.minimall.notify.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.notify.entity.ComplaintPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 投诉 MyBatis Mapper 接口，映射 t_notify_complaint 表
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Mapper
public interface ComplaintMapper extends BaseMapper<ComplaintPO> {
}
