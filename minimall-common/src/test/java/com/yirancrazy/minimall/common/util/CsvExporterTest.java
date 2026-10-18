package com.yirancrazy.minimall.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CsvExporterTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class CsvExporterTest {


    @Test
    void escape_neutralizes_formula_prefixes() {
        assertEquals("'=1+1", CsvExporter.escape("=1+1"));
        assertEquals("'+1", CsvExporter.escape("+1"));
        assertEquals("'-1", CsvExporter.escape("-1"));
        assertEquals("'@SUM(A1:A9)", CsvExporter.escape("@SUM(A1:A9)"));
        assertEquals("'\tcmd", CsvExporter.escape("\tcmd"));
    }


    @Test
    void escape_quotes_delimiters() {
        assertEquals("\"a,b\"", CsvExporter.escape("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.escape("say \"hi\""));
        assertEquals("\"line1\nline2\"", CsvExporter.escape("line1\nline2"));
    }


    @Test
    void escape_neutralizes_formula_inside_quoted_field() {
        assertEquals("\"'=1+1,a\"", CsvExporter.escape("=1+1,a"));
    }


    @Test
    void escape_keeps_plain_values() {
        assertEquals("OD10001", CsvExporter.escape("OD10001"));
        assertEquals("99.99", CsvExporter.escape("99.99"));
        assertEquals("", CsvExporter.escape(""));
    }
}
