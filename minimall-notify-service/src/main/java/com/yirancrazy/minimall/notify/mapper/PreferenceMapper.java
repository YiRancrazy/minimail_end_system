package com.yirancrazy.minimall.notify.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.notify.entity.PreferencePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知偏好 MyBatis Mapper 接口，映射 t_notify_preference 表
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Mapper
public interface PreferenceMapper extends BaseMapper<PreferencePO> {
}
