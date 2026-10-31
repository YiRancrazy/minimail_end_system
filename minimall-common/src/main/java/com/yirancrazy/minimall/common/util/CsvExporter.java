package com.yirancrazy.minimall.common.util;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: CSV 导出工具，向 HTTP 响应写入 UTF-8 BOM、表头和数据行，并防范电子表格公式注入。
 * 该工具只处理响应流与格式转义，不负责数据查询、权限控制或导出条数裁剪。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
public final class CsvExporter {

  private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
  private static final String CONTENT_TYPE = "text/csv; charset=UTF-8";
  private static final int MAX_EXPORT_ROWS = 10000;

  /** CSV 导出工具类禁止实例化。 */
  private CsvExporter() {}

  /**
   * 设置 CSV 响应头和 UTF-8 BOM，依次写入表头与数据行并关闭包装后的 Writer。
   *
   * @param response 当前 HTTP 响应
   * @param filename 下载文件名，换行和双引号会被移除
   * @param headers 表头字段
   * @param rows 数据行集合
   * @throws IOException 写入响应流失败时抛出
   */
  public static void write(
      HttpServletResponse response, String filename, String[] headers, List<String[]> rows)
      throws IOException {
    response.setContentType(CONTENT_TYPE);
    response.setHeader("Content-Disposition", buildContentDisposition(filename));
    OutputStream os = response.getOutputStream();
    os.write(UTF8_BOM);
    try (Writer writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
      writeRow(writer, headers);
      for (String[] row : rows) {
        writeRow(writer, row);
      }
    }
    log.info("csv 导出完成，filename={}，rows={}", filename, rows.size());
  }

  /**
   * 返回单次同步导出的建议最大行数。
   *
   * @return 固定上限 10000
   */
  public static int maxExportRows() {
    return MAX_EXPORT_ROWS;
  }

  /**
   * 清理文件名中的换行和双引号，并按 RFC 5987 形式编码 UTF-8 下载名称。
   *
   * @param filename 原始下载文件名
   * @return Content-Disposition 请求头值
   */
  private static String buildContentDisposition(String filename) {
    String safe = filename.replace("\r", "").replace("\n", "").replace("\"", "");
    String encoded = URLEncoder.encode(safe, StandardCharsets.UTF_8).replace("+", "%20");
    return "attachment; filename*=UTF-8''" + encoded;
  }

  /**
   * 按 CSV 规则写入一行字段，字段之间使用逗号分隔，行尾使用换行符。
   *
   * @param writer 响应 Writer
   * @param fields 当前行字段，null 字段按空字符串处理
   * @throws IOException 写入失败时抛出
   */
  private static void writeRow(Writer writer, String[] fields) throws IOException {
    for (int i = 0; i < fields.length; i++) {
      if (i > 0) {
        writer.write(",");
      }
      writer.write(escape(fields[i] == null ? "" : fields[i]));
    }
    writer.write("\n");
  }

  /**
   * 转义 CSV 字段并中和公式前缀；含逗号、双引号或换行时使用双引号包裹。
   *
   * @param field 原始字段值
   * @return 可安全写入 CSV 的字段文本
   */
  static String escape(String field) {
    String safe = neutralizeFormula(field);
    if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
      return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
    return safe;
  }

  /**
   * 对以等号、加号、减号、@、制表符或回车开头的字段添加单引号，阻止电子表格公式执行。
   *
   * @param field 原始字段值
   * @return 中和公式前缀后的字段
   */
  private static String neutralizeFormula(String field) {
    if (field.isEmpty()) {
      return field;
    }
    char first = field.charAt(0);
    if (first == '='
        || first == '+'
        || first == '-'
        || first == '@'
        || first == '\t'
        || first == '\r') {
      return "'" + field;
    }
    return field;
  }
}
