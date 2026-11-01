package com.yirancrazy.minimall.notify.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.constant.CommentStatusEnum;
import com.yirancrazy.minimall.notify.dto.CommentPageDto;
import com.yirancrazy.minimall.notify.service.CommentService;
import com.yirancrazy.minimall.notify.vo.CommentStatsVo;
import com.yirancrazy.minimall.notify.vo.CommentVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供无需评价归属的公开商品评价查询和评分统计接口，列表只返回正常状态评价.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/goods/comments")
public class PublicCommentControllerV1 {

    private final CommentService commentService;

    /**
     * 注入评价业务服务.
     *
     * @param commentService 公开评价查询入口
     */
    public PublicCommentControllerV1(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 查询指定 SPU 的公开评价游标分页，并由接口强制状态为 NORMAL.
     *
     * @param spuId 商品 SPU 编号
     * @param dto 游标及公开评价查询条件
     * @return 当前页公开评价及下一页游标
     */
    @GetMapping("/{spuId}")
    public Result<CursorPageVo<CommentVo>> pageBySpu(
            @PathVariable Long spuId, @Valid CommentPageDto dto) {
        dto.setSpuId(spuId);
        dto.setStatus(CommentStatusEnum.NORMAL.intCode());
        return Result.success(commentService.pageVo(dto));
    }

    /**
     * 查询指定 SPU 的正常评价总数、平均分和各评分数量.
     *
     * @param spuId 商品 SPU 编号
     * @return 评分统计视图
     */
    @GetMapping("/{spuId}/_stats")
    public Result<CommentStatsVo> stats(@PathVariable Long spuId) {
        return Result.success(commentService.stats(spuId));
    }
}
