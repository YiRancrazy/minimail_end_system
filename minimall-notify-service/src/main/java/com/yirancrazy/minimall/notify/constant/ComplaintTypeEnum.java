package com.yirancrazy.minimall.notify.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义投诉单可选的业务分类，用于区分质量、服务、欺诈及其他投诉场景.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum ComplaintTypeEnum implements BaseEnum {
  QUALITY("QUALITY", "QUALITY", "质量问题"),
  SERVICE("SERVICE", "SERVICE", "服务问题"),
  FRAUD("FRAUD", "FRAUD", "欺诈行为"),
  OTHER("OTHER", "OTHER", "其他");

  private final String code;
  private final String alias;
  private final String message;
}
