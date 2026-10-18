package com.yirancrazy.minimall.common.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: CsvExporter 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
public final class CsvExporter {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String CONTENT_TYPE = "text/csv; charset=UTF-8";
    private static final int MAX_EXPORT_ROWS = 10000;

    /**
     * 构造 CsvExporter 实例。
     */
    private CsvExporter() {
    }


    /**
     * 执行 write 方法。
     * @param response response 参数
     * @param filename filename 参数
     * @param headers headers 参数
     * @param rows rows 参数
     * @throws IOException 处理失败时抛出
     */
    public static void write(HttpServletResponse response, String filename,
                             String[] headers, List<String[]> rows) throws IOException {
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
     * 执行 maxExportRows 方法。
     * @return 业务数值
     */
    public static int maxExportRows() {
        return MAX_EXPORT_ROWS;
    }


    /**
     * 构建或生成ContentDisposition。
     * @param filename filename
     * @return 处理结果
     */
    private static String buildContentDisposition(String filename) {
        String safe = filename.replace("\r", "").replace("\n", "").replace("\"", "");
        String encoded = URLEncoder.encode(safe, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }

    /**
     * 处理writeRow。
     * @param writer writer
     * @param fields fields
     * @throws IOException 处理失败时抛出
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
     * 处理escape。
     * @param field field
     * @return 处理结果
     */
    static String escape(String field) {
        String safe = neutralizeFormula(field);
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }


    /**
     * 处理neutralizeFormula。
     * @param field field
     * @return 处理结果
     */
    private static String neutralizeFormula(String field) {
        if (field.isEmpty()) {
            return field;
        }
        char first = field.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@'
            || first == '\t' || first == '\r') {
            return "'" + field;
        }
        return field;
    }
}
