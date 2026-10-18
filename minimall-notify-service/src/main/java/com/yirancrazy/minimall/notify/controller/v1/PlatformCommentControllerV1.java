package com.yirancrazy.minimall.notify.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.dto.CommentReplyDTO;
import com.yirancrazy.minimall.notify.service.PlatformCommentService;
import com.yirancrazy.minimall.notify.vo.CommentVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformCommentControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/platform/comments")
public class PlatformCommentControllerV1 {

    private final PlatformCommentService platformCommentService;

    /**
     * 构造 PlatformCommentControllerV1 实例。
     * @param platformCommentService platformCommentService 参数
     */
    public PlatformCommentControllerV1(PlatformCommentService platformCommentService) {
        this.platformCommentService = platformCommentService;
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<CursorPageVO<CommentVO>> page(@Valid CommentPageDTO dto) {
        return Result.success(platformCommentService.pageVO(dto));
    }


    /**
     * 执行 approve 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable("id") Long id) {
        platformCommentService.approve(id);
        return Result.success(null);
    }


    /**
     * 执行 hide 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @PostMapping("/{id}/hide")
    public Result<Void> hide(@PathVariable("id") Long id) {
        platformCommentService.hide(id);
        return Result.success(null);
    }


    /**
     * 执行 reply 方法。
     * @param id id 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable("id") Long id,
                              @Valid @RequestBody CommentReplyDTO dto) {
        platformCommentService.reply(id, dto.getReply());
        return Result.success(null);
    }
}
