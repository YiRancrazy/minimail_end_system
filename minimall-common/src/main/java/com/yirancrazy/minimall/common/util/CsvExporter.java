package com.yirancrazy.minimall.common.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CSV 导出工具，UTF-8 BOM 编码，支持字段转义。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Slf4j
public final class CsvExporter {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String CONTENT_TYPE = "text/csv; charset=UTF-8";
    private static final int MAX_EXPORT_ROWS = 10000;

    private CsvExporter() {
    }

    /**
     * 写入 CSV 到响应流，含 UTF-8 BOM 头与表头。
     * @param response HTTP 响应
     * @param filename 下载文件名
     * @param headers 表头数组
     * @param rows 数据行列表，每行为字段数组
     * @throws IOException 写入失败时抛出
     */
    public static void write(HttpServletResponse response, String filename,
                             String[] headers, List<String[]> rows) throws IOException {
        response.setContentType(CONTENT_TYPE);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        OutputStream os = response.getOutputStream();
        os.write(UTF8_BOM);
        try (Writer writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
            writeRow(writer, headers);
            for (String[] row : rows) {
                writeRow(writer, row);
            }
        }
        log.info("csv exported, filename={}, rows={}", filename, rows.size());
    }

    /**
     * 获取导出最大行数限制。
     * @return 最大行数
     */
    public static int maxExportRows() {
        return MAX_EXPORT_ROWS;
    }

    private static void writeRow(Writer writer, String[] fields) throws IOException {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                writer.write(",");
            }
            writer.write(escape(fields[i] == null ? "" : fields[i]));
        }
        writer.write("\n");
    }

    private static String escape(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
