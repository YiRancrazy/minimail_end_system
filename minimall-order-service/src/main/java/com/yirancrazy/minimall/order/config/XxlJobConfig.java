package com.yirancrazy.minimall.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: XxlJobConfig 配置类。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
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
     * 执行 xxlJobExecutor 方法。
     * @return 处理结果
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
