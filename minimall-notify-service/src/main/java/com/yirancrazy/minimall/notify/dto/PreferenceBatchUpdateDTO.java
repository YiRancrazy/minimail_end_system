package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载一次请求中提交的多条通知偏好变更，逐项校验后在同一事务内更新.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class PreferenceBatchUpdateDto {

  @NotEmpty(message = "items cannot be empty")
  @Valid
  private List<PreferenceUpdateDto> items;
}
