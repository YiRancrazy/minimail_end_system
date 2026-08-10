package com.yirancrazy.minimall.notify.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.notify.entity.CommentPO;
import com.yirancrazy.minimall.notify.manager.CommentManager;
import com.yirancrazy.minimall.notify.mapper.CommentMapper;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品评价数据访问层实现，封装 t_notify_comment 表 CRUD 操作
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Manager
public class CommentManagerImpl extends ServiceImpl<CommentMapper, CommentPO>
    implements CommentManager {
}
