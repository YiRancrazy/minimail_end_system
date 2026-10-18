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
import com.yirancrazy.minimall.notify.service.ComplaintService;
import com.yirancrazy.minimall.notify.vo.ComplaintVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserComplaintControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/user/complaints")
public class UserComplaintControllerV1 {

    private final ComplaintService complaintService;

    /**
     * 构造 UserComplaintControllerV1 实例。
     * @param complaintService complaintService 参数
     */
    public UserComplaintControllerV1(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }


    /**
     * 分页查询数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<CursorPageVO<ComplaintVO>> page(@RequestHeader("X-User-Id") Long userId,
                                                  @Valid ComplaintPageDTO dto) {
        // 强制覆盖 userId 字段，防止前端传入他人 ID 越权查询
        dto.setUserId(userId);
        return Result.success(complaintService.pageVO(dto));
    }


    /**
     * 执行 detail 方法。
     * @param id id 参数
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping("/{id}")
    public Result<ComplaintVO> detail(@PathVariable("id") Long id,
                                      @RequestHeader("X-User-Id") Long userId) {
        return Result.success(complaintService.detailForUser(id, userId));
    }
}
