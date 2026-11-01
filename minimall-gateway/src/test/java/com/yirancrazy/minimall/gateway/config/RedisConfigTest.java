package com.yirancrazy.minimall.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证网关 Redis 密码来自环境配置，并验证 docker-compose 提供预期的
 *   REDIS_PASSWORD 变量。测试仅读取仓库文件，不启动 Redis 或网关应用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class RedisConfigTest {

    private static final String REDIS_PASSWORD_PLACEHOLDER = "${REDIS_PASSWORD:}";

    /** 验证网关从环境变量读取 Redis 密码；前置为类路径存在配置；执行加载 YAML；预期密码为环境占位符. */
    @Test
    void testGatewayReadsRedisPasswordFromEnvironment() throws IOException {
        List<PropertySource<?>> sources =
                new YamlPropertySourceLoader()
                        .load("application", new ClassPathResource("application.yml"));
        assertEquals(1, sources.size());
        assertEquals(
                REDIS_PASSWORD_PLACEHOLDER, sources.get(0).getProperty("spring.data.redis.password"));
    }

    /** 验证 Compose 统一注入 Redis 密码；前置为仓库存在编排文件；执行解析共享环境；预期密码使用默认回退表达式. */
    @Test
    void testComposeInjectsRedisPasswordIntoApps() throws IOException {
        Path compose = locateRepositoryFile("docker-compose.yml");
        assumeTrue(compose != null, "docker-compose.yml only exists inside the repository");
        Map<String, Object> root = new Yaml().load(Files.readString(compose));
        Object appEnv = root.get("x-app-env");
        assertNotNull(appEnv, "x-app-env anchor must exist");
        assertEquals("${REDIS_PASSWORD:-redis123}", ((Map<?, ?>) appEnv).get("REDIS_PASSWORD"));
    }

    /**
     * 从当前目录逐级向上查找仓库文件。
     *
     * @param fileName 要查找的文件名
     * @return 第一个匹配的普通文件；所有父目录均不存在时返回 null
     */
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
