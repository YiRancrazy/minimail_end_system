package com.yirancrazy.minimall.notify.manager.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yirancrazy.minimall.common.annotation.Manager;
import com.yirancrazy.minimall.notify.dto.CommentRatingCountDto;
import com.yirancrazy.minimall.notify.entity.CommentPo;
import com.yirancrazy.minimall.notify.manager.CommentManager;
import com.yirancrazy.minimall.notify.mapper.CommentMapper;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 基于 CommentMapper 实现评价记录的 CRUD 和评分分组统计，仅负责数据库访问，不处理评价业务规则.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Manager
public class CommentManagerImpl extends ServiceImpl<CommentMapper, CommentPo>
    implements CommentManager {

  /**
   * 将评分分组统计请求直接委托给 CommentMapper.
   *
   * @param spuId 商品 SPU 编号
   * @param status 需要统计的评价状态编码
   * @return 各评分值对应的评价数量
   */
  @Override
  public List<CommentRatingCountDto> countGroupByRating(Long spuId, Integer status) {
    return baseMapper.countGroupByRating(spuId, status);
  }
}
