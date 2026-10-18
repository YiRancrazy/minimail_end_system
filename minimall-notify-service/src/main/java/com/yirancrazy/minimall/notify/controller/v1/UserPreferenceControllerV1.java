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
 * @Description: UserPreferenceControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/user/preferences")
public class UserPreferenceControllerV1 {

    private final PreferenceService preferenceService;

    /**
     * 构造 UserPreferenceControllerV1 实例。
     * @param preferenceService preferenceService 参数
     */
    public UserPreferenceControllerV1(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }


    /**
     * 查询列表数据。
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<List<PreferenceVO>> list(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(preferenceService.listByUser(userId));
    }


    /**
     * 更新业务数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping
    public Result<Void> updateBatch(@RequestHeader("X-User-Id") Long userId,
                                   @Valid @RequestBody PreferenceBatchUpdateDTO dto) {
        preferenceService.updateBatch(userId, dto.getItems());
        return Result.success(null);
    }
}
