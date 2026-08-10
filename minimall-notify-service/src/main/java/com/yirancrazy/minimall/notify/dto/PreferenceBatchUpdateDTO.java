package com.yirancrazy.minimall.notify.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知偏好批量更新 DTO，封装多条 PreferenceUpdateDTO
 * @Version: 1.0
 * @DateTime: 2026/08/10
 **/
@Data
public class PreferenceBatchUpdateDTO {

    @NotEmpty(message = "items cannot be empty")
    @Valid
    private List<PreferenceUpdateDTO> items;
}
