package com.yirancrazy.minimall.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
}