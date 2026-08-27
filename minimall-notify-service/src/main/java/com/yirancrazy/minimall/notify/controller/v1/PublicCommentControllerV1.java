package com.yirancrazy.minimall.notify.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.constant.CommentStatusEnum;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.service.CommentService;
import com.yirancrazy.minimall.notify.vo.CommentStatsVO;
import com.yirancrazy.minimall.notify.vo.CommentVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 公开商品评价控制器，商品详情页可匿名访问评价列表与统计
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@RestController
@RequestMapping("/api/v1/goods/comments")
public class PublicCommentControllerV1 {

    private final CommentService commentService;
    private final MinioUtil minioUtil;

    public PublicCommentControllerV1(CommentService commentService, MinioUtil minioUtil) {
        this.commentService = commentService;
        this.minioUtil = minioUtil;
    }

    /**
     * 公开接口：按 SPU 查询正常状态的评价列表。
     * @param spuId 商品SPU ID
     * @param dto 分页入参
     * @return 评价分页结果
     */
    @GetMapping("/{spuId}")
    public Result<CursorPageVO<CommentVO>> pageBySpu(@PathVariable Long spuId,
                                                     @Valid CommentPageDTO dto) {
        dto.setSpuId(spuId);
        dto.setStatus(CommentStatusEnum.NORMAL.intCode());
        return Result.success(commentService.page(dto).map(po -> CommentVO.from(po, minioUtil)));
    }

    /**
     * 公开接口：按 SPU 查询评价统计。
     * @param spuId 商品SPU ID
     * @return 评价统计视图
     */
    @GetMapping("/{spuId}/_stats")
    public Result<CommentStatsVO> stats(@PathVariable Long spuId) {
        return Result.success(commentService.stats(spuId));
    }
}
