package com.yirancrazy.minimall.notify.manager;

import java.util.List;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.notify.dto.CommentRatingCountDTO;
import com.yirancrazy.minimall.notify.entity.CommentPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CommentManager 管理器接口，封装数据访问能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface CommentManager extends IService<CommentPO> {


    /**
     * 统计业务数据。
     * @param spuId spuId 参数
     * @param status status 参数
     * @return 结果列表
     */
    List<CommentRatingCountDTO> countGroupByRating(Long spuId, Integer status);
}
