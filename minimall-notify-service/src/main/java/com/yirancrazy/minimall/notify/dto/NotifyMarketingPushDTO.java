package com.yirancrazy.minimall.notify.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyMarketingPushDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class NotifyMarketingPushDTO {

    @NotBlank(message = "title cannot be blank")
    private String title;

    @NotBlank(message = "content cannot be blank")
    private String content;


    private List<Long> userIds;
}
