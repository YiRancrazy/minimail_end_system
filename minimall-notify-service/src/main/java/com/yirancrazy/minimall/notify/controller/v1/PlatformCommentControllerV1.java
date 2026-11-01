package com.yirancrazy.minimall.notify.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.dto.CommentPageDto;
import com.yirancrazy.minimall.notify.dto.CommentReplyDto;
import com.yirancrazy.minimall.notify.service.PlatformCommentService;
import com.yirancrazy.minimall.notify.vo.CommentVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供平台侧评价游标查询、恢复展示、隐藏和回复接口，统一以 Result 包装响应并由服务层校验评价是否存在.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/platform/comments")
public class PlatformCommentControllerV1 {

    private final PlatformCommentService platformCommentService;

    /**
     * 注入平台评价业务服务.
     *
     * @param platformCommentService 平台评价审核、隐藏和回复入口
     */
    public PlatformCommentControllerV1(PlatformCommentService platformCommentService) {
        this.platformCommentService = platformCommentService;
    }

    /**
     * 查询平台评价游标分页，可按状态和订单号过滤.
     *
     * @param dto 游标及平台评价筛选条件
     * @return 当前页评价视图及下一页游标
     */
    @GetMapping
    public Result<CursorPageVo<CommentVo>> page(@Valid CommentPageDto dto) {
        return Result.success(platformCommentService.pageVo(dto));
    }

    /**
     * 将指定评价恢复为正常状态；已正常时服务层直接返回.
     *
     * @param id 评价主键
     * @return 无业务数据的成功响应
     */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable("id") Long id) {
        platformCommentService.approve(id);
        return Result.success(null);
    }

    /**
     * 将指定评价隐藏。
     *
     * @param id 评价主键
     * @return 无业务数据的成功响应
     */
    @PostMapping("/{id}/hide")
    public Result<Void> hide(@PathVariable("id") Long id) {
        platformCommentService.hide(id);
        return Result.success(null);
    }

    /**
     * 以平台身份回复指定评价并记录回复时间.
     *
     * @param id 评价主键
     * @param dto 平台回复正文
     * @return 无业务数据的成功响应
     */
    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable("id") Long id, @Valid @RequestBody CommentReplyDto dto) {
        platformCommentService.reply(id, dto.getReply());
        return Result.success(null);
    }
}
