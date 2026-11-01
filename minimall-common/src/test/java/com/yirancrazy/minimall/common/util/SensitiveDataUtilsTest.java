package com.yirancrazy.minimall.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证手机号、身份证、银行卡、密码和令牌在各输入边界下的脱敏规则。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@DisplayName("脱敏工具类测试")
class SensitiveDataUtilsTest {

    /** * 验证空手机号；输入null；调用脱敏；预期返回null. */
    @Test
    @DisplayName("手机号脱敏 - null输入")
    void testMaskPhoneNullInput() {
        assertNull(SensitiveDataUtils.maskPhone(null));
    }

    /** * 验证空手机号；输入空串；调用脱敏；预期返回空串. */
    @Test
    @DisplayName("手机号脱敏 - 空字符串")
    void testMaskPhoneEmptyString() {
        assertEquals("", SensitiveDataUtils.maskPhone(""));
    }

    /** * 验证短手机号；输入138和12345；调用脱敏；预期原样返回. */
    @Test
    @DisplayName("手机号脱敏 - 短字符串")
    void testMaskPhoneShortString() {
        assertEquals("138", SensitiveDataUtils.maskPhone("138"));
        assertEquals("12345", SensitiveDataUtils.maskPhone("12345"));
    }

    /** * 验证标准手机号；输入两个11位号码；调用脱敏；预期保留前三后四. */
    @Test
    @DisplayName("手机号脱敏 - 正常11位手机号")
    void testMaskPhoneNormalPhone() {
        assertEquals("138****5678", SensitiveDataUtils.maskPhone("13812345678"));
        assertEquals("159****0000", SensitiveDataUtils.maskPhone("15912340000"));
    }

    /** * 验证分隔符手机号；输入短横线或空格格式；调用脱敏；预期输出同一掩码. */
    @Test
    @DisplayName("手机号脱敏 - 包含非数字字符")
    void testMaskPhoneNonDigitCharacters() {
        assertEquals("138****5678", SensitiveDataUtils.maskPhone("138-1234-5678"));
        assertEquals("138****5678", SensitiveDataUtils.maskPhone("138 1234 5678"));
    }

    /** * 验证空证件号；输入null；调用脱敏；预期返回null. */
    @Test
    @DisplayName("身份证脱敏 - null输入")
    void testMaskIdCardNullInput() {
        assertNull(SensitiveDataUtils.maskIdCard(null));
    }

    /** * 验证空证件号；输入空串；调用脱敏；预期返回空串. */
    @Test
    @DisplayName("身份证脱敏 - 空字符串")
    void testMaskIdCardEmptyString() {
        assertEquals("", SensitiveDataUtils.maskIdCard(""));
    }

    /** * 验证短证件号；输入320和320106；调用脱敏；预期原样返回. */
    @Test
    @DisplayName("身份证脱敏 - 短字符串")
    void testMaskIdCardShortString() {
        assertEquals("320", SensitiveDataUtils.maskIdCard("320"));
        assertEquals("320106", SensitiveDataUtils.maskIdCard("320106"));
    }

    /** * 验证18位证件号；输入两个标准号码；调用脱敏；预期中间11位替换星号. */
    @Test
    @DisplayName("身份证脱敏 - 正常18位身份证")
    void testMaskIdCardNormal18DigitIdCard() {
        assertEquals("320***********1234", SensitiveDataUtils.maskIdCard("320106199001011234"));
        assertEquals("110***********5678", SensitiveDataUtils.maskIdCard("110101199001015678"));
    }

    /** * 验证15位证件号；输入两个旧号码；调用脱敏；预期中间8位替换星号. */
    @Test
    @DisplayName("身份证脱敏 - 15位老身份证")
    void testMaskIdCardOld15DigitIdCard() {
        assertEquals("320********1123", SensitiveDataUtils.maskIdCard("320106900101123"));
        assertEquals("110********1567", SensitiveDataUtils.maskIdCard("110101900101567"));
    }

    /** * 验证非数字证件号；输入混合分隔字符；调用脱敏；预期按数字序列处理. */
    @Test
    @DisplayName("身份证脱敏 - 包含非数字字符")
    void testMaskIdCardNonDigitCharacters() {
        assertEquals("320***********1234", SensitiveDataUtils.maskIdCard("320106199001011234"));
    }

    /** * 验证空银行卡；输入null；调用脱敏；预期返回null. */
    @Test
    @DisplayName("银行卡号脱敏 - null输入")
    void testMaskBankCardNullInput() {
        assertNull(SensitiveDataUtils.maskBankCard(null));
    }

    /** * 验证空银行卡；输入空串；调用脱敏；预期返回空串. */
    @Test
    @DisplayName("银行卡号脱敏 - 空字符串")
    void testMaskBankCardEmptyString() {
        assertEquals("", SensitiveDataUtils.maskBankCard(""));
    }

    /** * 验证短银行卡；输入6222和622202；调用脱敏；预期原样返回. */
    @Test
    @DisplayName("银行卡号脱敏 - 短字符串")
    void testMaskBankCardShortString() {
        assertEquals("6222", SensitiveDataUtils.maskBankCard("6222"));
        assertEquals("622202", SensitiveDataUtils.maskBankCard("622202"));
    }

    /** * 验证16位银行卡；输入两个标准卡号；调用脱敏；预期中间8位替换星号. */
    @Test
    @DisplayName("银行卡号脱敏 - 正常16位银行卡号")
    void testMaskBankCardNormal16DigitBankCard() {
        assertEquals("6222********1234", SensitiveDataUtils.maskBankCard("6222021234561234"));
        assertEquals("6225********5678", SensitiveDataUtils.maskBankCard("6225881234565678"));
    }

    /** * 验证分隔符银行卡；输入短横线或空格格式；调用脱敏；预期同一掩码输出. */
    @Test
    @DisplayName("银行卡号脱敏 - 包含非数字字符")
    void testMaskBankCardNonDigitCharacters() {
        assertEquals("6222********1234", SensitiveDataUtils.maskBankCard("6222-0212-3456-1234"));
        assertEquals("6222********1234", SensitiveDataUtils.maskBankCard("6222 0212 3456 1234"));
    }

    /** * 验证空密码；输入null；调用脱敏；预期返回null. */
    @Test
    @DisplayName("密码脱敏 - null输入")
    void testMaskPasswordNullInput() {
        assertNull(SensitiveDataUtils.maskPassword(null));
    }

    /** * 验证空密码；输入空串；调用脱敏；预期返回空串. */
    @Test
    @DisplayName("密码脱敏 - 空字符串")
    void testMaskPasswordEmptyString() {
        assertEquals("", SensitiveDataUtils.maskPassword(""));
    }

    /** * 验证非空密码；输入短中长三种值；调用脱敏；预期统一六个星号. */
    @Test
    @DisplayName("密码脱敏 - 正常密码")
    void testMaskPasswordNormalPassword() {
        assertEquals("******", SensitiveDataUtils.maskPassword("password123"));
        assertEquals("******", SensitiveDataUtils.maskPassword("abc"));
        assertEquals("******", SensitiveDataUtils.maskPassword("verylongpassword"));
    }

    /** * 验证空令牌；输入null；调用脱敏；预期返回null. */
    @Test
    @DisplayName("Token脱敏 - null输入")
    void testMaskTokenNullInput() {
        assertNull(SensitiveDataUtils.maskToken(null));
    }

    /** * 验证空令牌；输入空串；调用脱敏；预期返回空串. */
    @Test
    @DisplayName("Token脱敏 - 空字符串")
    void testMaskTokenEmptyString() {
        assertEquals("", SensitiveDataUtils.maskToken(""));
    }

    /** * 验证短令牌；输入六位和八位值；调用脱敏；预期原样返回. */
    @Test
    @DisplayName("Token脱敏 - 短字符串")
    void testMaskTokenShortString() {
        assertEquals("abc123", SensitiveDataUtils.maskToken("abc123"));
        assertEquals("12345678", SensitiveDataUtils.maskToken("12345678"));
    }

    /** * 验证长令牌；输入两个长令牌；调用脱敏；预期保留前四后四并遮蔽中间. */
    @Test
    @DisplayName("Token脱敏 - 正常Token")
    void testMaskTokenNormalToken() {
        String token1 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        String expected1 =
                "eyJh" + "*".repeat(token1.length() - 8) + token1.substring(token1.length() - 4);
        assertEquals(expected1, SensitiveDataUtils.maskToken(token1));

        String token2 = "abcd1234567890xyz";
        String expected2 =
                "abcd" + "*".repeat(token2.length() - 8) + token2.substring(token2.length() - 4);
        assertEquals(expected2, SensitiveDataUtils.maskToken(token2));
    }
}
