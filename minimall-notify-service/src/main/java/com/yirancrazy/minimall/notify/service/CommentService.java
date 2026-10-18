package com.yirancrazy.minimall.notify.service;

import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.notify.dto.CommentCreateDTO;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.entity.CommentPO;
import com.yirancrazy.minimall.notify.vo.CommentStatsVO;
import com.yirancrazy.minimall.notify.vo.CommentVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CommentService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface CommentService {


    /**
     * 创建业务数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 业务数值
     */
    Long create(Long userId, CommentCreateDTO dto);



    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<CommentPO> page(CommentPageDTO dto);
    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<CommentVO> pageVO(CommentPageDTO dto);


    /**
     * 执行 reply 方法。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @param reply reply 参数
     */
    void reply(Long id, Long merchantId, String reply);


    /**
     * 执行 stats 方法。
     * @param spuId spuId 参数
     * @return 处理结果
     */
    CommentStatsVO stats(Long spuId);
}
