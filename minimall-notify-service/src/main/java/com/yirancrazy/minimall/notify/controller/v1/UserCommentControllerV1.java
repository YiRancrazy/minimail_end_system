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
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.dto.CommentCreateDTO;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.service.CommentService;
import com.yirancrazy.minimall.notify.vo.CommentVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户端商品评价控制器，提供提交评价与查询「我的评价」能力
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@RestController
@RequestMapping("/api/v1/user/comments")
public class UserCommentControllerV1 {

    private final CommentService commentService;
    private final MinioUtil minioUtil;

    public UserCommentControllerV1(CommentService commentService, MinioUtil minioUtil) {
        this.commentService = commentService;
        this.minioUtil = minioUtil;
    }

    /**
     * 用户提交商品评价。
     * @param userId 用户ID（Header 注入）
     * @param dto 创建入参
     * @return 新评价ID
     */
    @PostMapping
    public Result<Long> create(@RequestHeader("X-User-Id") Long userId,
                               @Valid @RequestBody CommentCreateDTO dto) {
        return Result.success(commentService.create(userId, dto));
    }

    /**
     * 用户查询自己的评价列表。
     * @param userId 用户ID（Header 注入）
     * @param dto 分页入参
     * @return 评价分页结果
     */
    @GetMapping
    public Result<CursorPageVO<CommentVO>> page(@RequestHeader("X-User-Id") Long userId,
                                                @Valid CommentPageDTO dto) {
        dto.setUserId(userId);
        return Result.success(commentService.page(dto).map(po -> CommentVO.from(po, minioUtil)));
    }
}
