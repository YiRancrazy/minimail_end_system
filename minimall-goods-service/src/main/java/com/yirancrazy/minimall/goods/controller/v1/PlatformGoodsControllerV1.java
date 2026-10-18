package com.yirancrazy.minimall.goods.controller.v1;

import java.util.List;
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
import com.yirancrazy.minimall.goods.dto.SpuPageDTO;
import com.yirancrazy.minimall.goods.dto.SpuRejectDTO;
import com.yirancrazy.minimall.goods.service.SpuService;
import com.yirancrazy.minimall.goods.vo.SpuAuditRecordVO;
import com.yirancrazy.minimall.goods.vo.SpuVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformGoodsControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/platform/goods")
public class PlatformGoodsControllerV1 {

    private final SpuService spuService;

    /**
     * 构造 PlatformGoodsControllerV1 实例。
     * @param spuService spuService 参数
     */
    public PlatformGoodsControllerV1(SpuService spuService) {
        this.spuService = spuService;
    }


    /**
     * 执行 pending 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/spus/pending")
    public Result<CursorPageVO<SpuVO>> pending(@Valid SpuPageDTO dto) {
        return Result.success(spuService.pendingVO(dto));
    }


    /**
     * 执行 detail 方法。
     * @param spuId spuId 参数
     * @return 统一响应结果
     */
    @GetMapping("/spus/{spuId}")
    public Result<SpuVO> detail(@PathVariable("spuId") Long spuId) {
        return Result.success(spuService.detailVO(spuId));
    }


    /**
     * 执行 auditLogs 方法。
     * @param spuId spuId 参数
     * @return 统一响应结果
     */
    @GetMapping("/spus/{spuId}/audit-logs")
    public Result<List<SpuAuditRecordVO>> auditLogs(@PathVariable("spuId") Long spuId) {
        return Result.success(spuService.listAuditRecordsVO(spuId));
    }


    /**
     * 执行 approve 方法。
     * @param spuId spuId 参数
     * @param auditorId auditorId 参数
     * @return 统一响应结果
     */
    @PostMapping("/spus/{spuId}/approve")
    public Result<Boolean> approve(@PathVariable("spuId") Long spuId,
                                   @RequestHeader("X-User-Id") Long auditorId) {
        return Result.success(spuService.approve(spuId, auditorId));
    }


    /**
     * 执行 reject 方法。
     * @param spuId spuId 参数
     * @param auditorId auditorId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/spus/{spuId}/reject")
    public Result<Boolean> reject(@PathVariable("spuId") Long spuId,
                                  @RequestHeader("X-User-Id") Long auditorId,
                                  @Valid @RequestBody SpuRejectDTO dto) {
        return Result.success(spuService.reject(spuId, auditorId, dto.getReason()));
    }


    /**
     * 执行 auditRecords 方法。
     * @param spuId spuId 参数
     * @return 统一响应结果
     */
    @GetMapping("/spus/{spuId}/audit-records")
    public Result<List<SpuAuditRecordVO>> auditRecords(@PathVariable("spuId") Long spuId) {
        return Result.success(spuService.listAuditRecordsVO(spuId));
    }
}
