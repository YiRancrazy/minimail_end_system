package com.yirancrazy.minimall.notify.manager.impl;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.notify.dto.CommentRatingCountDTO;
import com.yirancrazy.minimall.notify.entity.CommentPO;
import com.yirancrazy.minimall.notify.manager.CommentManager;
import com.yirancrazy.minimall.notify.mapper.CommentMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CommentManagerImpl 管理器实现，封装数据访问逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Manager
public class CommentManagerImpl extends ServiceImpl<CommentMapper, CommentPO>
    implements CommentManager {

    /**
     * 统计业务数据。
     * @param spuId spuId 参数
     * @param status status 参数
     * @return 结果列表
     */
    @Override
    public List<CommentRatingCountDTO> countGroupByRating(Long spuId, Integer status) {
        return baseMapper.countGroupByRating(spuId, status);
    }
}
