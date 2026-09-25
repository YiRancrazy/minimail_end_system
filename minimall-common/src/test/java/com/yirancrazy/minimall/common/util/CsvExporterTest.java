package com.yirancrazy.minimall.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: CsvExporter 单元测试类，校验公式注入中和与 RFC 4180 字段转义。
 * @Version: 1.0
 * @DateTime: 2026/09/25
 **/
class CsvExporterTest {

    /**
     * 验证以 = + - @ 等公式前缀开头的字段被前置单引号中和。
     */
    @Test
    void escape_neutralizes_formula_prefixes() {
        assertEquals("'=1+1", CsvExporter.escape("=1+1"));
        assertEquals("'+1", CsvExporter.escape("+1"));
        assertEquals("'-1", CsvExporter.escape("-1"));
        assertEquals("'@SUM(A1:A9)", CsvExporter.escape("@SUM(A1:A9)"));
        assertEquals("'\tcmd", CsvExporter.escape("\tcmd"));
    }

    /**
     * 验证含逗号、双引号、换行的字段按 RFC 4180 加引号并转义内部引号。
     */
    @Test
    void escape_quotes_delimiters() {
        assertEquals("\"a,b\"", CsvExporter.escape("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.escape("say \"hi\""));
        assertEquals("\"line1\nline2\"", CsvExporter.escape("line1\nline2"));
    }

    /**
     * 验证公式前缀与分隔符同时出现时先中和再整体加引号。
     */
    @Test
    void escape_neutralizes_formula_inside_quoted_field() {
        assertEquals("\"'=1+1,a\"", CsvExporter.escape("=1+1,a"));
    }

    /**
     * 验证普通字段保持原样，避免误伤业务单号与金额。
     */
    @Test
    void escape_keeps_plain_values() {
        assertEquals("OD10001", CsvExporter.escape("OD10001"));
        assertEquals("99.99", CsvExporter.escape("99.99"));
        assertEquals("", CsvExporter.escape(""));
    }
}
