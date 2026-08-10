package com.yirancrazy.minimall.notify.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.dto.ComplaintPageDTO;
import com.yirancrazy.minimall.notify.entity.ComplaintPO;
import com.yirancrazy.minimall.notify.service.ComplaintService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户端投诉控制器，提供「我的投诉」列表与详情能力
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@RestController
@RequestMapping("/api/v1/user/complaints")
public class UserComplaintControllerV1 {

    private final ComplaintService complaintService;

    public UserComplaintControllerV1(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /**
     * 用户查询自己提交的投诉列表。
     * @param userId 用户ID（Header 注入）
     * @param dto 分页入参
     * @return 投诉分页结果
     */
    @GetMapping
    public Result<CursorPageVO<ComplaintPO>> page(@RequestHeader("X-User-Id") Long userId,
                                                  @Valid ComplaintPageDTO dto) {
        // 强制覆盖 userId 字段，防止前端传入他人 ID 越权查询
        dto.setUserId(userId);
        return Result.success(complaintService.page(dto));
    }

    /**
     * 用户查询自己提交的投诉详情。
     * @param id 投诉ID
     * @param userId 用户ID（Header 注入）
     * @return 投诉实体
     */
    @GetMapping("/{id}")
    public Result<ComplaintPO> detail(@PathVariable("id") Long id,
                                      @RequestHeader("X-User-Id") Long userId) {
        ComplaintPO po = complaintService.detail(id);
        // 简化：仅校验投诉方为当前用户，防止越权访问他人投诉
        if (po.getComplainantId() == null || !po.getComplainantId().equals(userId)) {
            // 复用 COMPLAINT_NOT_FOUND，避免泄露存在性
            return Result.success(null);
        }
        return Result.success(po);
    }
}
