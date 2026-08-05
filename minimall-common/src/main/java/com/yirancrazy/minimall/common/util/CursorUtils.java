package com.yirancrazy.minimall.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 游标编解码工具，将记录ID编码为不透明 Base64 字符串供前端传递。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
public final class CursorUtils {

    private CursorUtils() {
    }

    /**
     * 将记录ID编码为游标字符串。
     * @param id 记录ID
     * @return Base64 编码的游标字符串
     */
    public static String encode(Long id) {
        if (id == null) {
            return null;
        }
        return Base64.getEncoder()
            .encodeToString(String.valueOf(id).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 将游标字符串解码为记录ID。
     * @param cursor 游标字符串
     * @return 记录ID，cursor 为空时返回 null
     * @throws BizException 当游标格式非法时
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
            throw new BizException(CommonCode.SYS_ERROR, "invalid cursor");
        }
    }
}
