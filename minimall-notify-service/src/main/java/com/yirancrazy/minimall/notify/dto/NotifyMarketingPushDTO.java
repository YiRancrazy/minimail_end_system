package com.yirancrazy.minimall.notify.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 营销推送入参，支持指定用户列表或全量广播
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
public class NotifyMarketingPushDTO {

    @NotBlank(message = "title cannot be blank")
    private String title;

    @NotBlank(message = "content cannot be blank")
    private String content;

    /** 目标用户ID列表；null 表示全量广播（落库 userId=0 占位） */
    private List<Long> userIds;
}
