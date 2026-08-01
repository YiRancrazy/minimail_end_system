package com.yirancrazy.minimall.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Load .env file and set as system properties, only from file not system env.
 * @Version: 1.1
 * @DateTime: 2026/08/01
 */
@Slf4j
public class DotenvConfig implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Path envFile = findEnvFile();
        if (envFile == null) {
            log.info(".env file not found, skipping");
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
                // remove surrounding quotes
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                System.setProperty(key, value);
                count++;
                log.debug("Loaded .env: {}={}", key, key.contains("PASSWORD") ? "***" : value);
            }
            log.info("Loaded {} entries from .env file", count);
        } catch (IOException e) {
            log.warn("Failed to read .env file: {}", e.getMessage());
        }
    }

    private Path findEnvFile() {
        // search from working dir up to 3 levels
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
}