package com.yirancrazy.minimall.notify.manager;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.notify.entity.CommentPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品评价数据访问层接口，定义 t_notify_comment 表操作契约
 * @Version: 1.1
 * @DateTime: 2026/08/10
 **/
public interface CommentManager extends IService<CommentPO> {

    /**
     * 按评分分组统计指定 SPU 的正常评价数量，聚合在数据库侧完成。
     * @param spuId 商品 SPU ID
     * @param status 评价状态过滤（NORMAL）
     * @return 每行 {rating, cnt} 的分组结果
     */
    List<Map<String, Object>> countGroupByRating(Long spuId, Integer status);
}
