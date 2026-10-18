package com.yirancrazy.minimall.notify.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.dto.CommentCreateDTO;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.service.CommentService;
import com.yirancrazy.minimall.notify.vo.CommentVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserCommentControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/user/comments")
public class UserCommentControllerV1 {

    private final CommentService commentService;

    /**
     * 构造 UserCommentControllerV1 实例。
     * @param commentService commentService 参数
     */
    public UserCommentControllerV1(CommentService commentService) {
        this.commentService = commentService;
    }


    /**
     * 创建业务数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping
    public Result<Long> create(@RequestHeader("X-User-Id") Long userId,
                               @Valid @RequestBody CommentCreateDTO dto) {
        return Result.success(commentService.create(userId, dto));
    }


    /**
     * 分页查询数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<CursorPageVO<CommentVO>> page(@RequestHeader("X-User-Id") Long userId,
                                                @Valid CommentPageDTO dto) {
        dto.setUserId(userId);
        return Result.success(commentService.pageVO(dto));
    }
}
