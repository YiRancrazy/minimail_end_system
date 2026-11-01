package com.yirancrazy.minimall.order.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 按配置装配 XXL-Job 订单服务执行器。
 *   业务边界：仅在 minimall.xxl-job.enabled=true 时生效，负责连接注册中心及设置日志目录，不包含任务调度业务。
 *   使用场景：部署侧启用 XXL-Job 调度时使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "minimall.xxl-job.enabled", havingValue = "true")
public class XxlJobConfig {

    @Value("${minimall.xxl-job.admin-addresses:}")
    private String adminAddresses;

    @Value("${minimall.xxl-job.access-token:}")
    private String accessToken;

    @Value("${minimall.xxl-job.app-name:minimall-order-service}")
    private String appName;

    @Value("${minimall.xxl-job.ip:}")
    private String ip;

    @Value("${minimall.xxl-job.port:9999}")
    private int port;

    @Value("${minimall.xxl-job.log-path:/data/applogs/xxl-job/order}")
    private String logPath;

    @Value("${minimall.xxl-job.log-retention-days:30}")
    private int logRetentionDays;

    /**
     * 创建并配置 XXL-Job 执行器。
     *
     * <p>执行器地址、令牌、应用名、IP、端口和日志参数均来自 minimall.xxl-job 配置，Bean 仅在显式启用时加载。
     *
     * @return 配置完成的 XxlJobSpringExecutor。
     */
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info("xxl-job 执行器初始化，admin={}，app={}", adminAddresses, appName);
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAccessToken(accessToken);
        executor.setAppname(appName);
        executor.setIp(ip);
        executor.setPort(port);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        return executor;
    }
}
