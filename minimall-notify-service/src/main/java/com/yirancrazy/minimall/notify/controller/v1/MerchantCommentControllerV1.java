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
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.notify.dto.CommentPageDTO;
import com.yirancrazy.minimall.notify.dto.CommentReplyDTO;
import com.yirancrazy.minimall.notify.entity.CommentPO;
import com.yirancrazy.minimall.notify.service.CommentService;
import com.yirancrazy.minimall.notify.vo.CommentVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家端商品评价控制器，提供商家维度评价分页与回复能力
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@RestController
@RequestMapping("/api/v1/merchant/comments")
public class MerchantCommentControllerV1 {

    private final CommentService commentService;
    private final MinioUtil minioUtil;

    public MerchantCommentControllerV1(CommentService commentService, MinioUtil minioUtil) {
        this.commentService = commentService;
        this.minioUtil = minioUtil;
    }

    /**
     * 商家查询本店评价列表。
     * @param merchantId 商家ID（Header 注入）
     * @param dto 分页入参
     * @return 评价分页结果
     */
    @GetMapping
    public Result<CursorPageVO<CommentVO>> page(@RequestHeader("X-Merchant-Id") Long merchantId,
                                                @Valid CommentPageDTO dto) {
        dto.setMerchantId(merchantId);
        return Result.success(commentService.page(dto).map(po -> CommentVO.from(po, minioUtil)));
    }

    /**
     * 商家回复评价，仅限同 merchantId 的评价。
     * @param id 评价ID
     * @param merchantId 商家ID（Header 注入）
     * @param dto 回复入参
     * @return 操作结果
     */
    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id,
                              @RequestHeader("X-Merchant-Id") Long merchantId,
                              @Valid @RequestBody CommentReplyDTO dto) {
        commentService.reply(id, merchantId, dto.getReply());
        return Result.success(null);
    }
}
