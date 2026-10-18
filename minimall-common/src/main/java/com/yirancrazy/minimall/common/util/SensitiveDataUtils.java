package com.yirancrazy.minimall.common.util;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SensitiveDataUtils 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public final class SensitiveDataUtils {

    /**
     * 构造 SensitiveDataUtils 实例。
     */
    private SensitiveDataUtils() {
    }


    /**
     * 执行 maskPhone 方法。
     * @param phone phone 参数
     * @return 处理结果
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
     * 执行 maskIdCard 方法。
     * @param idCard idCard 参数
     * @return 处理结果
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.isEmpty()) {
            return idCard;
        }

        String digits = idCard.replaceAll("[^0-9]", "");
        if (digits.length() < 7) {
            return idCard;
        }

        int length = digits.length();
        String stars = "*".repeat(length - 7);
        return digits.substring(0, 3) + stars + digits.substring(length - 4);
    }


    /**
     * 执行 maskBankCard 方法。
     * @param bankCard bankCard 参数
     * @return 处理结果
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
     * 执行 maskPassword 方法。
     * @param password password 参数
     * @return 处理结果
     */
    public static String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return password;
        }
        return "******";
    }


    /**
     * 执行 maskToken 方法。
     * @param token token 参数
     * @return 处理结果
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
