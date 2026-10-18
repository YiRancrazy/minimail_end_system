package com.yirancrazy.minimall.notify.service;

import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.entity.CommentPO;
import com.yirancrazy.minimall.notify.vo.CommentVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformCommentService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface PlatformCommentService {



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
     * 执行 approve 方法。
     * @param id id 参数
     */
    void approve(Long id);


    /**
     * 执行 hide 方法。
     * @param id id 参数
     */
    void hide(Long id);


    /**
     * 执行 reply 方法。
     * @param id id 参数
     * @param reply reply 参数
     */
    void reply(Long id, String reply);
}
