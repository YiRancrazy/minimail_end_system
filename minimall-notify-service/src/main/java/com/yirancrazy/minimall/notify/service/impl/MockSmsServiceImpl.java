package com.yirancrazy.minimall.notify.service.impl;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.notify.service.SmsService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MockSmsServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
@ConditionalOnProperty(name = "minimall.notify.sms.enabled", havingValue = "false", matchIfMissing = true)
public class MockSmsServiceImpl implements SmsService {

    /**
     * 发送业务消息。
     * @param phone phone 参数
     * @param templateCode templateCode 参数
     * @param params params 参数
     * @return 是否处理成功
     */
    @Override
    public boolean send(String phone, String templateCode, Map<String, String> params) {
        log.info("mock 短信已发送，phone={}，template={}，params={}", maskPhone(phone), templateCode, params);
        return true;
    }

    /**
     * 处理maskPhone。
     * @param phone phone
     * @return 处理结果
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
