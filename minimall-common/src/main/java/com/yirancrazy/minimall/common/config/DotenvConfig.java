package com.yirancrazy.minimall.common.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Spring 启动早期的 .env 文件加载器，从工作目录向上最多三级查找环境文件，
 * 将键值写入 System properties，并在日志中屏蔽密码、密钥和令牌。用于本地开发配置补充。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
public class DotenvConfig implements ApplicationContextInitializer {

    /**
     * 查找并加载 .env 配置到 JVM 系统属性。文件不存在、读取失败或单行格式无效时跳过；
     * 已存在的同名系统属性会被文件值覆盖。
     *
     * @param applicationContext 当前 Spring 应用上下文，本实现不读取其中的 Bean
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
        } catch (IOException e) {
            log.warn("读取 .env 文件失败: {}", e.getMessage());
        }
    }

    /**
     * 从当前工作目录开始向上最多搜索三层，返回第一个存在的 .env 文件。
     *
     * @return .env 文件路径；未找到时返回 null
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
     * 根据配置键名判断是否为敏感配置，敏感项固定返回三个星号，避免日志泄露值。
     *
     * @param key 配置键名，大小写不敏感
     * @param value 原始配置值
     * @return 敏感键返回 {@code ***}，其余键返回原值
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
