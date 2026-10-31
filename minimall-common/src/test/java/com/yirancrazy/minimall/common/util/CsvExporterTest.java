package com.yirancrazy.minimall.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 CSV 字段转义、引号包裹和电子表格公式注入防护。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class CsvExporterTest {

  /** * 验证公式中和；输入五类危险前缀；调用转义；预期全部加单引号. */
  @Test
  void testEscapeNeutralizesFormulaPrefixes() {
    assertEquals("'=1+1", CsvExporter.escape("=1+1"));
    assertEquals("'+1", CsvExporter.escape("+1"));
    assertEquals("'-1", CsvExporter.escape("-1"));
    assertEquals("'@SUM(A1:A9)", CsvExporter.escape("@SUM(A1:A9)"));
    assertEquals("'\tcmd", CsvExporter.escape("\tcmd"));
  }

  /** * 验证CSV转义；字段含逗号引号换行；调用转义；预期双引号包裹并成对转义. */
  @Test
  void testEscapeQuotesDelimiters() {
    assertEquals("\"a,b\"", CsvExporter.escape("a,b"));
    assertEquals("\"say \"\"hi\"\"\"", CsvExporter.escape("say \"hi\""));
    assertEquals("\"line1\nline2\"", CsvExporter.escape("line1\nline2"));
  }

  /** * 验证复合转义；字段公式开头且含逗号；调用转义；预期先加单引号再整体包裹. */
  @Test
  void testEscapeNeutralizesFormulaInsideQuotedField() {
    assertEquals("\"'=1+1,a\"", CsvExporter.escape("=1+1,a"));
  }

  /** * 验证普通值透传；输入编码金额和空串；调用转义；预期原样返回. */
  @Test
  void testEscapeKeepsPlainValues() {
    assertEquals("OD10001", CsvExporter.escape("OD10001"));
    assertEquals("99.99", CsvExporter.escape("99.99"));
    assertEquals("", CsvExporter.escape(""));
  }
}
