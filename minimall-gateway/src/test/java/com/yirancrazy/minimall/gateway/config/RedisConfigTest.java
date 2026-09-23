package com.yirancrazy.minimall.gateway.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Redis 配置测试，校验网关密码只从环境变量注入且编排文件已下发该变量。
 * @Version: 1.0
 * @DateTime: 2026/09/23
 **/
class RedisConfigTest {

    private static final String REDIS_PASSWORD_PLACEHOLDER = "${REDIS_PASSWORD:}";

    @Test
    void gateway_reads_redis_password_from_environment() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
            .load("application", new ClassPathResource("application.yml"));
        assertEquals(1, sources.size());
        assertEquals(REDIS_PASSWORD_PLACEHOLDER,
            sources.get(0).getProperty("spring.data.redis.password"));
    }

    @Test
    void compose_injects_redis_password_into_apps() throws IOException {
        Path compose = locateRepositoryFile("docker-compose.yml");
        assumeTrue(compose != null, "docker-compose.yml only exists inside the repository");
        Map<String, Object> root = new Yaml().load(Files.readString(compose));
        Object appEnv = root.get("x-app-env");
        assertNotNull(appEnv, "x-app-env anchor must exist");
        assertEquals("${REDIS_PASSWORD:-redis123}", ((Map<?, ?>) appEnv).get("REDIS_PASSWORD"));
    }

    private static Path locateRepositoryFile(String fileName) {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve(fileName);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return null;
    }
}
