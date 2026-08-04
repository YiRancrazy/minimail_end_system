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

    /**
     * 银行卡号脱敏
     * <p>保留前4位和后4位，中间用*替换</p>
     *
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.isEmpty()) {
            return bankCard;
        }

        String digits = bankCard.replaceAll("[^0-9]", "");
        if (digits.length() < 8) {
            return bankCard;
        }

        int length = digits.length();
        String stars = "*".repeat(length - 8);
        return digits.substring(0, 4) + stars + digits.substring(length - 4);
    }

    /**
     * 密码脱敏
     * <p>完全遮蔽密码，返回固定长度的*</p>
     *
     * @param password 密码
     * @return 固定长度的遮蔽字符串
     */
    public static String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return password;
        }
        return "******";
    }

    /**
     * Token脱敏
     * <p>保留前4位和后4位，中间用*替换</p>
     *
     * @param token Token
     * @return 脱敏后的Token
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return token;
        }

        if (token.length() <= 8) {
            return token;
        }

        int length = token.length();
        String stars = "*".repeat(length - 8);
        return token.substring(0, 4) + stars + token.substring(length - 4);
    }
}