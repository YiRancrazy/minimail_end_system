package com.yirancrazy.minimall.common.util;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 日志和接口输出使用的敏感信息脱敏工具，覆盖手机号、身份证、银行卡、密码和令牌。
 * 规则以保留少量首尾字符为主；输入过短时按安全策略原样返回或固定掩码。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public final class SensitiveDataUtils {

  /** 脱敏工具类禁止实例化。 */
  private SensitiveDataUtils() {}

  /**
   * 移除非数字字符后保留手机号前三位和后四位；数字不足七位时原样返回。
   *
   * @param phone 原始手机号，可为 null 或空
   * @return 脱敏手机号或原始短值
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
   * 数字不足七位时原样返回；否则保留身份证前三位和后四位，中间使用星号替换。
   *
   * @param idCard 原始身份证号
   * @return 脱敏身份证号或原始短值
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
   * 数字不足八位时原样返回；否则保留银行卡前四位和后四位，中间使用星号替换。
   *
   * @param bankCard 原始银行卡号
   * @return 脱敏银行卡号或原始短值
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
   * 非空密码统一替换为六个星号，不保留任何原始字符。
   *
   * @param password 原始密码
   * @return 固定掩码；null 或空字符串原样返回
   */
  public static String maskPassword(String password) {
    if (password == null || password.isEmpty()) {
      return password;
    }
    return "******";
  }

  /**
   * 长度超过八位时保留令牌前四位和后四位；较短令牌原样返回。
   *
   * @param token 原始令牌
   * @return 脱敏令牌或原始短值
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
