package com.yirancrazy.minimall.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: SensitiveDataUtils 的单元测试类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@DisplayName("脱敏工具类测试")
class SensitiveDataUtilsTest {

    @Test
    @DisplayName("手机号脱敏 - null输入")
    void maskPhone_nullInput() {
        assertNull(SensitiveDataUtils.maskPhone(null));
    }

    @Test
    @DisplayName("手机号脱敏 - 空字符串")
    void maskPhone_emptyString() {
        assertEquals("", SensitiveDataUtils.maskPhone(""));
    }

    @Test
    @DisplayName("手机号脱敏 - 短字符串")
    void maskPhone_shortString() {
        assertEquals("138", SensitiveDataUtils.maskPhone("138"));
        assertEquals("12345", SensitiveDataUtils.maskPhone("12345"));
    }

    @Test
    @DisplayName("手机号脱敏 - 正常11位手机号")
    void maskPhone_normalPhone() {
        assertEquals("138****5678", SensitiveDataUtils.maskPhone("13812345678"));
        assertEquals("159****0000", SensitiveDataUtils.maskPhone("15912340000"));
    }

    @Test
    @DisplayName("手机号脱敏 - 包含非数字字符")
    void maskPhone_nonDigitCharacters() {
        assertEquals("138****5678", SensitiveDataUtils.maskPhone("138-1234-5678"));
        assertEquals("138****5678", SensitiveDataUtils.maskPhone("138 1234 5678"));
    }

    @Test
    @DisplayName("身份证脱敏 - null输入")
    void maskIdCard_nullInput() {
        assertNull(SensitiveDataUtils.maskIdCard(null));
    }

    @Test
    @DisplayName("身份证脱敏 - 空字符串")
    void maskIdCard_emptyString() {
        assertEquals("", SensitiveDataUtils.maskIdCard(""));
    }

    @Test
    @DisplayName("身份证脱敏 - 短字符串")
    void maskIdCard_shortString() {
        assertEquals("320", SensitiveDataUtils.maskIdCard("320"));
        assertEquals("320106", SensitiveDataUtils.maskIdCard("320106"));
    }

    @Test
    @DisplayName("身份证脱敏 - 正常18位身份证")
    void maskIdCard_normal18DigitIdCard() {
        assertEquals("320106********1234", SensitiveDataUtils.maskIdCard("320106199001011234"));
        assertEquals("110101********5678", SensitiveDataUtils.maskIdCard("110101199001015678"));
    }

    @Test
    @DisplayName("身份证脱敏 - 15位老身份证")
    void maskIdCard_old15DigitIdCard() {
        assertEquals("320106*****1123", SensitiveDataUtils.maskIdCard("320106900101123"));
        assertEquals("110101*****1567", SensitiveDataUtils.maskIdCard("110101900101567"));
    }

    @Test
    @DisplayName("身份证脱敏 - 包含非数字字符")
    void maskIdCard_nonDigitCharacters() {
        assertEquals("320106********1234", SensitiveDataUtils.maskIdCard("320106199001011234"));
    }

    @Test
    @DisplayName("银行卡号脱敏 - null输入")
    void maskBankCard_nullInput() {
        assertNull(SensitiveDataUtils.maskBankCard(null));
    }

    @Test
    @DisplayName("银行卡号脱敏 - 空字符串")
    void maskBankCard_emptyString() {
        assertEquals("", SensitiveDataUtils.maskBankCard(""));
    }

    @Test
    @DisplayName("银行卡号脱敏 - 短字符串")
    void maskBankCard_shortString() {
        assertEquals("6222", SensitiveDataUtils.maskBankCard("6222"));
        assertEquals("622202", SensitiveDataUtils.maskBankCard("622202"));
    }

    @Test
    @DisplayName("银行卡号脱敏 - 正常16位银行卡号")
    void maskBankCard_normal16DigitBankCard() {
        assertEquals("6222********1234", SensitiveDataUtils.maskBankCard("6222021234561234"));
        assertEquals("6225********5678", SensitiveDataUtils.maskBankCard("6225881234565678"));
    }

    @Test
    @DisplayName("银行卡号脱敏 - 包含非数字字符")
    void maskBankCard_nonDigitCharacters() {
        assertEquals("6222********1234", SensitiveDataUtils.maskBankCard("6222-0212-3456-1234"));
        assertEquals("6222********1234", SensitiveDataUtils.maskBankCard("6222 0212 3456 1234"));
    }

    @Test
    @DisplayName("密码脱敏 - null输入")
    void maskPassword_nullInput() {
        assertNull(SensitiveDataUtils.maskPassword(null));
    }

    @Test
    @DisplayName("密码脱敏 - 空字符串")
    void maskPassword_emptyString() {
        assertEquals("", SensitiveDataUtils.maskPassword(""));
    }

    @Test
    @DisplayName("密码脱敏 - 正常密码")
    void maskPassword_normalPassword() {
        assertEquals("******", SensitiveDataUtils.maskPassword("password123"));
        assertEquals("******", SensitiveDataUtils.maskPassword("abc"));
        assertEquals("******", SensitiveDataUtils.maskPassword("verylongpassword"));
    }

    @Test
    @DisplayName("Token脱敏 - null输入")
    void maskToken_nullInput() {
        assertNull(SensitiveDataUtils.maskToken(null));
    }

    @Test
    @DisplayName("Token脱敏 - 空字符串")
    void maskToken_emptyString() {
        assertEquals("", SensitiveDataUtils.maskToken(""));
    }

    @Test
    @DisplayName("Token脱敏 - 短字符串")
    void maskToken_shortString() {
        assertEquals("abc123", SensitiveDataUtils.maskToken("abc123"));
        assertEquals("12345678", SensitiveDataUtils.maskToken("12345678"));
    }

    @Test
    @DisplayName("Token脱敏 - 正常Token")
    void maskToken_normalToken() {
        String token1 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        String expected1 = "eyJh" + "*".repeat(token1.length() - 8) + token1.substring(token1.length() - 4);
        assertEquals(expected1, SensitiveDataUtils.maskToken(token1));

        String token2 = "abcd1234567890xyz";
        String expected2 = "abcd" + "*".repeat(token2.length() - 8) + token2.substring(token2.length() - 4);
        assertEquals(expected2, SensitiveDataUtils.maskToken(token2));
    }
}