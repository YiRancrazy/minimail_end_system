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
import com.yirancrazy.minimall.notify.service.ComplaintService;
import com.yirancrazy.minimall.notify.vo.ComplaintVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ComplaintControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/platform/complaints")
public class ComplaintControllerV1 {

    private final ComplaintService complaintService;

    /**
     * 构造 ComplaintControllerV1 实例。
     * @param complaintService complaintService 参数
     */
    public ComplaintControllerV1(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }


    /**
     * 创建业务数据。
     * @param complainantId complainantId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping
    @Idempotent
    public Result<Long> create(@RequestHeader("X-User-Id") Long complainantId,
                               @Valid @RequestBody ComplaintCreateDTO dto) {
        return Result.success(complaintService.create(complainantId, dto));
    }


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<CursorPageVO<ComplaintVO>> page(@Valid ComplaintPageDTO dto) {
        return Result.success(complaintService.pageVO(dto));
    }


    /**
     * 处理业务事件。
     * @param id id 参数
     * @param handlerId handlerId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable("id") Long id,
                               @RequestHeader("X-User-Id") Long handlerId,
                               @Valid @RequestBody ComplaintHandleDTO dto) {
        complaintService.handle(id, handlerId, dto);
        return Result.success(null);
    }


    /**
     * 执行 detail 方法。
     * @param id id 参数
     * @return 统一响应结果
     */
    @GetMapping("/{id}")
    public Result<ComplaintVO> detail(@PathVariable("id") Long id) {
        return Result.success(complaintService.detailVO(id));
    }
}
