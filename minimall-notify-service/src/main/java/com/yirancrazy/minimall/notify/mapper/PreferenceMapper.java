package com.yirancrazy.minimall.notify.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.notify.entity.PreferencePO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PreferenceMapper 数据访问接口，映射数据库操作。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Mapper
public interface PreferenceMapper extends BaseMapper<PreferencePO> {
}
