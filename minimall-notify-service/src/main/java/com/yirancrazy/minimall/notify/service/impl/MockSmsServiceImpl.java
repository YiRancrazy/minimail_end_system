package com.yirancrazy.minimall.notify.service.impl;

import com.yirancrazy.minimall.notify.service.SmsService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在短信渠道未显式启用时提供日志型模拟发送，记录脱敏手机号、模板编码和参数，不调用外部短信网关.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
@ConditionalOnProperty(
        name = "minimall.notify.sms.enabled",
        havingValue = "false",
        matchIfMissing = true)
public class MockSmsServiceImpl implements SmsService {

    /**
     * 记录一条模拟短信发送成功日志并返回成功；手机号在输出前脱敏.
     *
     * @param phone 收件人手机号
     * @param templateCode 短信模板编码
     * @param params 模板变量集合
     * @return 固定返回 true
     */
    @Override
    public boolean send(String phone, String templateCode, Map<String, String> params) {
        log.info("mock 短信已发送，phone={}，template={}，params={}", maskPhone(phone), templateCode, params);
        return true;
    }

    /**
     * 将手机号转换为保留前三后四的日志脱敏格式；过短或空值返回 ***.
     *
     * @param phone 原始手机号
     * @return 脱敏手机号
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
