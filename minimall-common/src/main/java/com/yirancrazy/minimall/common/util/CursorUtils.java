package com.yirancrazy.minimall.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CursorUtils 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public final class CursorUtils {

    /**
     * 构造 CursorUtils 实例。
     */
    private CursorUtils() {
    }


    /**
     * 执行 encode 方法。
     * @param id id 参数
     * @return 处理结果
     */
    public static String encode(Long id) {
        if (id == null) {
            return null;
        }
        return Base64.getEncoder()
            .encodeToString(String.valueOf(id).getBytes(StandardCharsets.UTF_8));
    }


    /**
     * 执行 decode 方法。
     * @param cursor cursor 参数
     * @return 业务数值
     */
    public static Long decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(cursor);
            return Long.parseLong(new String(decoded, StandardCharsets.UTF_8));
        }
        catch (Exception e) {
            throw new BizException(CommonCode.PARAM_INVALID, "无效游标");
        }
    }
}
