package com.yirancrazy.minimall.notify.service;

import java.util.List;
import com.yirancrazy.minimall.notify.dto.PreferenceUpdateDTO;
import com.yirancrazy.minimall.notify.vo.PreferenceVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PreferenceService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface PreferenceService {


    /**
     * 查询列表数据。
     * @param userId userId 参数
     * @return 结果列表
     */
    List<PreferenceVO> listByUser(Long userId);


    /**
     * 更新业务数据。
     * @param userId userId 参数
     * @param items items 参数
     */
    void updateBatch(Long userId, List<PreferenceUpdateDTO> items);


    /**
     * 执行 isChannelEnabled 方法。
     * @param userId userId 参数
     * @param categoryCode categoryCode 参数
     * @param channel channel 参数
     * @return 是否处理成功
     */
    boolean isChannelEnabled(Long userId, String categoryCode, String channel);
}
