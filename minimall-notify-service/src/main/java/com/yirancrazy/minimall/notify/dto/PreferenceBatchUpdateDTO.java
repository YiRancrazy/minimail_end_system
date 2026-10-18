package com.yirancrazy.minimall.notify.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PreferenceBatchUpdateDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class PreferenceBatchUpdateDTO {

    @NotEmpty(message = "items cannot be empty")
    @Valid
    private List<PreferenceUpdateDTO> items;
}
