package com.yirancrazy.minimall.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: CursorUtils 单元测试，验证游标编解码与非法游标的错误码语义。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 */
class CursorUtilsTest {

    @Test
    void encode_then_decode_round_trip() {
        Long id = 1000000001L;
        assertEquals(id, CursorUtils.decode(CursorUtils.encode(id)));
    }

    @Test
    void encode_null_returns_null() {
        assertNull(CursorUtils.encode(null));
    }

    @Test
    void decode_blank_returns_null() {
        assertNull(CursorUtils.decode(null));
        assertNull(CursorUtils.decode("  "));
    }

    @Test
    void decode_illegalBase64_throwsParamInvalid() {
        BizException e = assertThrows(BizException.class, () -> CursorUtils.decode("!!!not-base64!!!"));
        assertEquals(CommonCode.PARAM_INVALID, e.getCode());
        assertEquals("无效游标", e.getMessage());
    }

    @Test
    void decode_nonNumericContent_throwsParamInvalid() {
        String cursor = Base64.getEncoder()
            .encodeToString("not-a-number".getBytes(StandardCharsets.UTF_8));
        BizException e = assertThrows(BizException.class, () -> CursorUtils.decode(cursor));
        assertEquals(CommonCode.PARAM_INVALID, e.getCode());
    }
}
