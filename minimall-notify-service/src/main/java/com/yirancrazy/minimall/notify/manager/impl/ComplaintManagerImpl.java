package com.yirancrazy.minimall.notify.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.notify.entity.ComplaintPO;
import com.yirancrazy.minimall.notify.manager.ComplaintManager;
import com.yirancrazy.minimall.notify.mapper.ComplaintMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 投诉数据访问层实现，封装 t_notify_complaint 表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Manager
public class ComplaintManagerImpl extends ServiceImpl<ComplaintMapper, ComplaintPO>
    implements ComplaintManager {
}
