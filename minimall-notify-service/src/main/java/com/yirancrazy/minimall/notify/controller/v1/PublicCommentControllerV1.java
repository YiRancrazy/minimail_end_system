package com.yirancrazy.minimall.notify.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.constant.CommentStatusEnum;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.service.CommentService;
import com.yirancrazy.minimall.notify.vo.CommentStatsVO;
import com.yirancrazy.minimall.notify.vo.CommentVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PublicCommentControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/goods/comments")
public class PublicCommentControllerV1 {

    private final CommentService commentService;

    /**
     * 构造 PublicCommentControllerV1 实例。
     * @param commentService commentService 参数
     */
    public PublicCommentControllerV1(CommentService commentService) {
        this.commentService = commentService;
    }


    /**
     * 分页查询数据。
     * @param spuId spuId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/{spuId}")
    public Result<CursorPageVO<CommentVO>> pageBySpu(@PathVariable Long spuId,
                                                     @Valid CommentPageDTO dto) {
        dto.setSpuId(spuId);
        dto.setStatus(CommentStatusEnum.NORMAL.intCode());
        return Result.success(commentService.pageVO(dto));
    }


    /**
     * 执行 stats 方法。
     * @param spuId spuId 参数
     * @return 统一响应结果
     */
    @GetMapping("/{spuId}/_stats")
    public Result<CommentStatsVO> stats(@PathVariable Long spuId) {
        return Result.success(commentService.stats(spuId));
    }
}
