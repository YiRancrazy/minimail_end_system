package com.yirancrazy.minimall.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证游标 Base64 编解码的往返、空值边界及非法游标的参数错误语义。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class CursorUtilsTest {

    /** * 验证游标往返；输入记录ID1000000001；编码后解码；预期还原原ID. */
    @Test
    void testEncodeThenDecodeRoundTrip() {
        Long id = 1000000001L;
        assertEquals(id, CursorUtils.decode(CursorUtils.encode(id)));
    }

    /** * 验证空ID处理；输入null；调用编码；预期返回null. */
    @Test
    void testEncodeNullReturnsNull() {
        assertNull(CursorUtils.encode(null));
    }

    /** * 验证空白游标处理；输入null和纯空格；分别解码；预期均返回null. */
    @Test
    void testDecodeBlankReturnsNull() {
        assertNull(CursorUtils.decode(null));
        assertNull(CursorUtils.decode("  "));
    }

    /** * 验证非法Base64；输入不可解码文本；调用解码；预期参数错误和无效游标消息. */
    @Test
    void testDecodeIllegalBase64ThrowsParamInvalid() {
        BizException e = assertThrows(BizException.class, () -> CursorUtils.decode("!!!not-base64!!!"));
        assertEquals(CommonCode.PARAM_INVALID, e.getCode());
        assertEquals("无效游标", e.getMessage());
    }

    /** * 验证非数字游标；Base64编码文本内容；调用解码；预期抛参数错误. */
    @Test
    void testDecodeNonNumericContentThrowsParamInvalid() {
        String cursor =
                Base64.getEncoder().encodeToString("not-a-number".getBytes(StandardCharsets.UTF_8));
        BizException e = assertThrows(BizException.class, () -> CursorUtils.decode(cursor));
        assertEquals(CommonCode.PARAM_INVALID, e.getCode());
    }
}
