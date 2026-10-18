package com.yirancrazy.minimall.notify.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.dto.CommentReplyDTO;
import com.yirancrazy.minimall.notify.service.CommentService;
import com.yirancrazy.minimall.notify.vo.CommentVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantCommentControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/merchant/comments")
public class MerchantCommentControllerV1 {

    private final CommentService commentService;

    /**
     * 构造 MerchantCommentControllerV1 实例。
     * @param commentService commentService 参数
     */
    public MerchantCommentControllerV1(CommentService commentService) {
        this.commentService = commentService;
    }


    /**
     * 分页查询数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<CursorPageVO<CommentVO>> page(@RequestHeader("X-Merchant-Id") Long merchantId,
                                                @Valid CommentPageDTO dto) {
        dto.setMerchantId(merchantId);
        return Result.success(commentService.pageVO(dto));
    }


    /**
     * 执行 reply 方法。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id,
                              @RequestHeader("X-Merchant-Id") Long merchantId,
                              @Valid @RequestBody CommentReplyDTO dto) {
        commentService.reply(id, merchantId, dto.getReply());
        return Result.success(null);
    }
}
