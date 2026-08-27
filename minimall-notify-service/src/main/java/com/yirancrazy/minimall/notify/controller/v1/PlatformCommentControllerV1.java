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
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.dto.CommentReplyDTO;
import com.yirancrazy.minimall.notify.service.PlatformCommentService;
import com.yirancrazy.minimall.notify.vo.CommentVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台评价审核控制器，提供分页查询、通过、隐藏、回复能力
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@RestController
@RequestMapping("/api/v1/platform/comments")
public class PlatformCommentControllerV1 {

    private final PlatformCommentService platformCommentService;
    private final MinioUtil minioUtil;

    public PlatformCommentControllerV1(PlatformCommentService platformCommentService, MinioUtil minioUtil) {
        this.platformCommentService = platformCommentService;
        this.minioUtil = minioUtil;
    }

    /**
     * 平台分页查询评价列表。
     * @param dto 分页入参
     * @return 评价分页结果
     */
    @GetMapping
    public Result<CursorPageVO<CommentVO>> page(@Valid CommentPageDTO dto) {
        return Result.success(platformCommentService.page(dto).map(po -> CommentVO.from(po, minioUtil)));
    }

    /**
     * 平台通过评价。
     * @param id 评价ID
     * @return 操作结果
     */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable("id") Long id) {
        platformCommentService.approve(id);
        return Result.success(null);
    }

    /**
     * 平台隐藏评价。
     * @param id 评价ID
     * @return 操作结果
     */
    @PostMapping("/{id}/hide")
    public Result<Void> hide(@PathVariable("id") Long id) {
        platformCommentService.hide(id);
        return Result.success(null);
    }

    /**
     * 平台回复评价。
     * @param id 评价ID
     * @param dto 回复入参
     * @return 操作结果
     */
    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable("id") Long id,
                              @Valid @RequestBody CommentReplyDTO dto) {
        platformCommentService.reply(id, dto.getReply());
        return Result.success(null);
    }
}
