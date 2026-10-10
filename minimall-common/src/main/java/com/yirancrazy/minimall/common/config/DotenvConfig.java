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
 * @Description: 加载 .env 文件并设置为系统属性，只读取文件，不读取系统环境变量。
 * @Version: 1.1
 * @DateTime: 2026/08/01
 */
@Slf4j
public class DotenvConfig implements ApplicationContextInitializer {

    @Override
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
     * 按配置键名脱敏敏感值，供启动日志安全输出。
     * @param key 配置键名
     * @param value 配置原始值
     * @return 敏感配置返回星号，否则返回原值
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
