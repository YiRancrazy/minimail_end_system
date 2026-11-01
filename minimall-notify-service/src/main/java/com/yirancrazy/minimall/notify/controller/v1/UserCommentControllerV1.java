package com.yirancrazy.minimall.notify.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.dto.CommentCreateDto;
import com.yirancrazy.minimall.notify.dto.CommentPageDto;
import com.yirancrazy.minimall.notify.service.CommentService;
import com.yirancrazy.minimall.notify.vo.CommentVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供用户创建评价和查询本人评价的接口，用户身份统一取自 X-User-Id 请求头.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/user/comments")
public class UserCommentControllerV1 {

    private final CommentService commentService;

    /**
     * 注入评价业务服务.
     *
     * @param commentService 用户评价创建和查询入口
     */
    public UserCommentControllerV1(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 为当前用户创建订单商品评价；购买资格、重复评价和商家归属由服务层校验.
     *
     * @param userId 当前登录用户编号
     * @param dto 评价订单、商品、评分、内容、图片和匿名标记
     * @return 新评价主键
     */
    @PostMapping
    public Result<Long> create(
            @RequestHeader("X-User-Id") Long userId, @Valid @RequestBody CommentCreateDto dto) {
        return Result.success(commentService.create(userId, dto));
    }

    /**
     * 查询当前用户自己的评价游标分页；请求头用户编号会覆盖查询参数中的 userId.
     *
     * @param userId 当前登录用户编号
     * @param dto 游标及评价筛选条件
     * @return 当前页评价视图及下一页游标
     */
    @GetMapping
    public Result<CursorPageVo<CommentVo>> page(
            @RequestHeader("X-User-Id") Long userId, @Valid CommentPageDto dto) {
        dto.setUserId(userId);
        return Result.success(commentService.pageVo(dto));
    }
}
