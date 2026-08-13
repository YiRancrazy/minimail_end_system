package com.yirancrazy.minimall.notify.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.annotation.Idempotent;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.dto.ComplaintCreateDTO;
import com.yirancrazy.minimall.notify.dto.ComplaintHandleDTO;
import com.yirancrazy.minimall.notify.dto.ComplaintPageDTO;
import com.yirancrazy.minimall.notify.entity.ComplaintPO;
import com.yirancrazy.minimall.notify.service.ComplaintService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 投诉控制器，提供投诉提交、分页查询、处理 RESTful API
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@RestController
@RequestMapping("/api/v1/platform/complaints")
public class ComplaintControllerV1 {

    private final ComplaintService complaintService;

    public ComplaintControllerV1(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /**
     * 提交投诉，complainantId 来自网关 X-User-Id 头。
     * @param complainantId 投诉方ID（Header 注入）
     * @param dto 创建入参
     * @return 新投诉ID
     */
    @PostMapping
    @Idempotent
    public Result<Long> create(@RequestHeader("X-User-Id") Long complainantId,
                               @Valid @RequestBody ComplaintCreateDTO dto) {
        return Result.success(complaintService.create(complainantId, dto));
    }

    /**
     * 平台分页查询投诉，支持按状态和订单号过滤。
     * @param dto 分页入参
     * @return 投诉分页结果
     */
    @GetMapping
    public Result<CursorPageVO<ComplaintPO>> page(@Valid ComplaintPageDTO dto) {
        return Result.success(complaintService.page(dto));
    }

    /**
     * 平台处理投诉，handlerId 来自网关 X-User-Id 头。
     * @param id 投诉ID
     * @param handlerId 处理人ID（Header 注入）
     * @param dto 处理入参
     * @return 无业务数据的成功响应
     */
    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable("id") Long id,
                               @RequestHeader("X-User-Id") Long handlerId,
                               @Valid @RequestBody ComplaintHandleDTO dto) {
        complaintService.handle(id, handlerId, dto);
        return Result.success(null);
    }

    /**
     * 平台查询投诉详情。
     * @param id 投诉ID
     * @return 投诉实体
     */
    @GetMapping("/{id}")
    public Result<ComplaintPO> detail(@PathVariable("id") Long id) {
        return Result.success(complaintService.detail(id));
    }
}
