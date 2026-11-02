package com.yirancrazy.minimall.notify.handle;

import com.yirancrazy.minimall.common.handle.IdempotencyContext;
import com.yirancrazy.minimall.common.handle.IdempotencyReplayHandler;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.entity.ComplaintPo;
import com.yirancrazy.minimall.notify.manager.ComplaintManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 按格式化幂等键回放首次投诉创建结果。
 * @Version: 1.0
 * @DateTime: 2026/11/2 9:16
 */
@Component
@RequiredArgsConstructor
public class ComplaintCreateReplayHandler implements IdempotencyReplayHandler {

    public static final String REPLAY_TYPE = "notify:complaint:create";

    private final ComplaintManager complaintManager;

    /**
     * 返回投诉创建业务类型。
     *
     * @return 投诉创建业务类型
     */
    @Override
    public String replayType() {
        return REPLAY_TYPE;
    }

    /**
     * 按幂等键查询首次投诉记录。
     *
     * @param context 幂等上下文，其幂等键为格式化后的投诉键
     * @return 投诉主键；记录尚未可见时返回 null
     */
    @Override
    public Result<Long> onDuplicate(IdempotencyContext context) {
        ComplaintPo complaint =
                complaintManager.findByIdempotencyKey(context.getIdempotencyKey());
        return complaint == null ? null : Result.success(complaint.getId());
    }
}
