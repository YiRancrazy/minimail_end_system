package com.yirancrazy.minimall.notify.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载用户批量删除站内通知的消息编号集合，单次最多允许 100 条.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class NotifyBatchDeleteDto {

    @NotEmpty(message = "消息ID列表不能为空")
    @Size(max = 100, message = "单次最多删除100条消息")
    private List<Long> ids;
}
