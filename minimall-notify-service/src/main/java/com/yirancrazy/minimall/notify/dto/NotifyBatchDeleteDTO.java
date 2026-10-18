package com.yirancrazy.minimall.notify.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyBatchDeleteDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class NotifyBatchDeleteDTO {

    @NotEmpty(message = "消息ID列表不能为空")
    @Size(max = 100, message = "单次最多删除100条消息")
    private List<Long> ids;
}
