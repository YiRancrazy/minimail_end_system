package com.yirancrazy.minimall.notify.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.dto.CommentPageDto;
import com.yirancrazy.minimall.notify.dto.CommentReplyDto;
import com.yirancrazy.minimall.notify.service.CommentService;
import com.yirancrazy.minimall.notify.vo.CommentVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供商家侧评价游标查询和回复接口，商家身份统一取自 X-Merchant-Id 请求头，避免前端伪造归属.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/merchant/comments")
public class MerchantCommentControllerV1 {

    private final CommentService commentService;

    /**
     * 注入评价业务服务.
     *
     * @param commentService 评价查询和回复入口
     */
    public MerchantCommentControllerV1(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 查询当前商家的评价游标分页；请求头商家编号会覆盖查询参数中的同名字段.
     *
     * @param merchantId 当前登录商家编号
     * @param dto 游标及评价筛选条件
     * @return 当前页评价视图及下一页游标
     */
    @GetMapping
    public Result<CursorPageVo<CommentVo>> page(
            @RequestHeader("X-Merchant-Id") Long merchantId, @Valid CommentPageDto dto) {
        dto.setMerchantId(merchantId);
        return Result.success(commentService.pageVo(dto));
    }

    /**
     * 以当前商家身份回复指定评价，服务层会再次校验评价归属.
     *
     * @param id 评价主键
     * @param merchantId 当前登录商家编号
     * @param dto 回复正文
     * @return 无业务数据的成功响应
     */
    @PostMapping("/{id}/reply")
    public Result<Void> reply(
            @PathVariable Long id,
            @RequestHeader("X-Merchant-Id") Long merchantId,
            @Valid @RequestBody CommentReplyDto dto) {
        commentService.reply(id, merchantId, dto.getReply());
        return Result.success(null);
    }
}
