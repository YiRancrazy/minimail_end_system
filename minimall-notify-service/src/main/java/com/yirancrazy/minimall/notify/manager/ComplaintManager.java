package com.yirancrazy.minimall.notify.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.notify.entity.ComplaintPo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 封装 t_notify_complaint 投诉记录的基础 MyBatis-Plus CRUD，不承载状态流转规则.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface ComplaintManager extends IService<ComplaintPo> {

    /**
     * 按幂等键查询投诉记录。
     *
     * @param idempotencyKey 格式化后的投诉幂等键
     * @return 已有投诉；不存在时返回 null
     */
    ComplaintPo findByIdempotencyKey(String idempotencyKey);
}
