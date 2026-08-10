package com.yirancrazy.minimall.notify.controller.v1;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.dto.PreferenceBatchUpdateDTO;
import com.yirancrazy.minimall.notify.service.PreferenceService;
import com.yirancrazy.minimall.notify.vo.PreferenceVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户端通知偏好控制器，提供查询与批量更新能力
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@RestController
@RequestMapping("/api/v1/user/preferences")
public class UserPreferenceControllerV1 {

    private final PreferenceService preferenceService;

    public UserPreferenceControllerV1(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    /**
     * 查询当前用户全部通知偏好，缺失类别返回默认值。
     * @param userId 用户ID（Header 注入）
     * @return 偏好视图列表
     */
    @GetMapping
    public Result<List<PreferenceVO>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(preferenceService.listByUser(userId));
    }

    /**
     * 批量更新当前用户通知偏好。
     * @param userId 用户ID（Header 注入）
     * @param dto 批量更新入参
     * @return 操作结果
     */
    @PostMapping
    public Result<Void> updateBatch(@RequestHeader("X-User-Id") Long userId,
                                   @Valid @RequestBody PreferenceBatchUpdateDTO dto) {
        preferenceService.updateBatch(userId, dto.getItems());
        return Result.success(null);
    }
}
