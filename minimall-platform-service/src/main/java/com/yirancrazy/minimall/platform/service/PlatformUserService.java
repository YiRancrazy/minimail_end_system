package com.yirancrazy.minimall.platform.service;

import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.platform.dto.PlatformUserPageDTO;
import com.yirancrazy.minimall.platform.vo.PlatformUserVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformUserService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface PlatformUserService {


    /**
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    CursorPageVO<PlatformUserVO> page(PlatformUserPageDTO dto);


    /**
     * 执行 detail 方法。
     * @param userId userId 参数
     * @return 处理结果
     */
    PlatformUserVO detail(Long userId);
}
