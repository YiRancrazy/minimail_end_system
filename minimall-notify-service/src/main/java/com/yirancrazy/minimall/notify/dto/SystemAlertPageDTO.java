package com.yirancrazy.minimall.notify.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.dto.CursorPageDTO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 系统告警分页查询入参，固定查询 PLATFORM + SYSTEM 类型消息
 * @Version: 1.1
 * @DateTime: 2026/08/04
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemAlertPageDTO extends CursorPageDTO {
}
