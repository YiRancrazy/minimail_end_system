package com.yirancrazy.minimall.common.util;

/**
 * 敏感数据脱敏工具类
 */
public final class SensitiveDataUtils {

    private SensitiveDataUtils() {
    }

    /**
     * 手机号脱敏
     * <p>保留前3位和后4位，中间用*替换</p>
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }

        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 7) {
            return phone;
        }

        return digits.substring(0, 3) + "****" + digits.substring(digits.length() - 4);
    }

    /**
     * 身份证号脱敏
     * <p>保留前6位和后4位，中间用*替换</p>
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.isEmpty()) {
            return idCard;
        }

        String digits = idCard.replaceAll("[^0-9]", "");
        if (digits.length() < 8) {
            return idCard;
        }

        int length = digits.length();
        String stars = "*".repeat(length - 10);
        return digits.substring(0, 6) + stars + digits.substring(length - 4);
    }
}