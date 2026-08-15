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
import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.service.SpuService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端商品审核控制器，提供待审核列表、通过、驳回、审核记录查询接口
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@RestController
@RequestMapping("/api/v1/platform/goods")
public class PlatformGoodsControllerV1 {

    private final SpuService spuService;

    public PlatformGoodsControllerV1(SpuService spuService) {
        this.spuService = spuService;
    }

    /**
     * 游标分页查询待审核 SPU 列表。
     * @param dto 游标分页查询入参
     * @return 待审核 SPU 游标分页结果
     */
    @GetMapping("/spus/pending")
    public Result<CursorPageVO<SpuPO>> pending(@Valid SpuPageDTO dto) {
        return Result.success(spuService.pagePending(dto));
    }

    /**
     * 查询 SPU 详情（审核页展示标题/描述用）。
     * @param spuId SPU 主键 ID
     * @return SPU 详情
     */
    @GetMapping("/spus/{spuId}")
    public Result<SpuPO> detail(@PathVariable("spuId") Long spuId) {
        return Result.success(spuService.getById(spuId));
    }

    /**
     * 查询指定 SPU 的审核记录（前端按 /audit-logs 契约调用）。
     * @param spuId SPU 主键 ID
     * @return 审核记录列表
     */
    @GetMapping("/spus/{spuId}/audit-logs")
    public Result<List<SpuAuditRecordPO>> auditLogs(@PathVariable("spuId") Long spuId) {
        return Result.success(spuService.listAuditRecords(spuId));
    }

    /**
     * 审核通过，将 SPU 从待审核置为在售。
     * @param spuId SPU 主键 ID
     * @param auditorId 审核员账号ID（来自网关 X-User-Id）
     * @return 审核是否成功
     */
    @PostMapping("/spus/{spuId}/approve")
    public Result<Boolean> approve(@PathVariable("spuId") Long spuId,
                                   @RequestHeader("X-User-Id") Long auditorId) {
        return Result.success(spuService.approve(spuId, auditorId));
    }

    /**
     * 审核驳回，将 SPU 从待审核置为驳回，必传驳回原因。
     * @param spuId SPU 主键 ID
     * @param auditorId 审核员账号ID（来自网关 X-User-Id）
     * @param dto 驳回入参
     * @return 审核是否成功
     */
    @PostMapping("/spus/{spuId}/reject")
    public Result<Boolean> reject(@PathVariable("spuId") Long spuId,
                                  @RequestHeader("X-User-Id") Long auditorId,
                                  @Valid @RequestBody SpuRejectDTO dto) {
        return Result.success(spuService.reject(spuId, auditorId, dto.getReason()));
    }

    /**
     * 查询指定 SPU 的审核记录列表。
     * @param spuId SPU 主键 ID
     * @return 审核记录列表
     */
    @GetMapping("/spus/{spuId}/audit-records")
    public Result<List<SpuAuditRecordPO>> auditRecords(@PathVariable("spuId") Long spuId) {
        return Result.success(spuService.listAuditRecords(spuId));
    }
}
