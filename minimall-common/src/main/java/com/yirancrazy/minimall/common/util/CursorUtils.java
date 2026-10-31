package com.yirancrazy.minimall.common.util;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 基于十进制 ID 的游标编解码工具，使用 Base64 避免把内部主键以明文形式直接暴露给调用方。
 * 编码不提供签名或加密能力，非法游标统一转换为参数错误。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public final class CursorUtils {

  /** 游标工具类禁止实例化。 */
  private CursorUtils() {}

  /**
   * 将 Long 类型的末条记录 ID 转为 Base64 文本游标。
   *
   * @param id 末条记录 ID
   * @return Base64 游标；id 为 null 时返回 null
   */
  public static String encode(Long id) {
    if (id == null) {
      return null;
    }
    return Base64.getEncoder().encodeToString(String.valueOf(id).getBytes(StandardCharsets.UTF_8));
  }

  /**
   * 解码 Base64 游标并解析为 Long 型记录 ID。
   *
   * @param cursor 客户端回传的游标
   * @return 记录 ID；cursor 为 null 或空白时返回 null
   * @throws BizException Base64 非法或内容不是有效 Long 时抛出
   */
  public static Long decode(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      byte[] decoded = Base64.getDecoder().decode(cursor);
      return Long.parseLong(new String(decoded, StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new BizException(CommonCode.PARAM_INVALID, "无效游标");
    }
  }
}
