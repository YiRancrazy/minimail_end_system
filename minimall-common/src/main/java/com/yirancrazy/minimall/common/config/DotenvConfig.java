package com.yirancrazy.minimall.common.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import lombok.extern.slf4j.Slf4j;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: DotenvConfig 配置类。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
public class DotenvConfig implements ApplicationContextInitializer {

    /**
     * 初始化相关资源。
     * @param applicationContext applicationContext 参数
     */
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Path envFile = findEnvFile();
        if (envFile == null) {
            log.info(".env 文件不存在，已跳过");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(envFile);
            int count = 0;
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
            // 去掉首尾引号
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                System.setProperty(key, value);
                count++;
                log.debug("已加载 .env: {}={}", key, redactedValue(key, value));
            }
            log.info("已从 .env 文件加载 {} 条配置", count);
        }
        catch (IOException e) {
            log.warn("读取 .env 文件失败: {}", e.getMessage());
        }
    }

    /**
     * 加载环境文件。
     * @return 查询结果
     */
    private Path findEnvFile() {
        // 从工作目录向上最多搜索 3 层
        Path dir = Paths.get(System.getProperty("user.dir"));
        for (int i = 0; i < 4; i++) {
            Path env = dir.resolve(".env");
            if (Files.exists(env)) {
                return env;
            }
            dir = dir.getParent();
            if (dir == null) {
                break;
            }
        }
        return null;
    }


    /**
     * 处理脱敏值。
     * @param key 键
     * @param value 值
     * @return 处理结果
     */
    static String redactedValue(String key, String value) {
        String normalizedKey = key == null ? "" : key.toUpperCase(Locale.ROOT);
        if (normalizedKey.contains("PASSWORD")
            || normalizedKey.contains("SECRET")
            || normalizedKey.contains("TOKEN")
            || normalizedKey.contains("PRIVATE_KEY")
            || normalizedKey.contains("ACCESS_KEY")
            || normalizedKey.contains("AES_KEY")) {
            return "***";
        }
        return value;
    }
}
