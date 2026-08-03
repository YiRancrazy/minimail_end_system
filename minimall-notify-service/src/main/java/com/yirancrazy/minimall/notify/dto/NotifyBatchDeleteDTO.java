package com.yirancrazy.minimall.notify.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 批量删除站内信入参，支持一次性软删除多条消息
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class NotifyBatchDeleteDTO {

    @NotEmpty(message = "消息ID列表不能为空")
    @Size(max = 100, message = "单次最多删除100条消息")
    private List<Long> ids;
}
