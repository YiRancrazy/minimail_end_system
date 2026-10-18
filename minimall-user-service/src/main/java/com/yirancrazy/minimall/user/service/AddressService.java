package com.yirancrazy.minimall.user.service;

import java.util.List;
import com.yirancrazy.minimall.user.dto.AddressCreateDTO;
import com.yirancrazy.minimall.user.dto.AddressUpdateDTO;
import com.yirancrazy.minimall.user.vo.AddressVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AddressService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface AddressService {


    /**
     * 查询列表数据。
     * @param userId userId 参数
     * @return 结果列表
     */
    List<AddressVO> listByUserId(Long userId);


    /**
     * 创建业务数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 业务数值
     */
    Long create(Long userId, AddressCreateDTO dto);


    /**
     * 更新业务数据。
     * @param userId userId 参数
     * @param id id 参数
     * @param dto dto 参数
     * @return 是否处理成功
     */
    boolean update(Long userId, Long id, AddressUpdateDTO dto);


    /**
     * 删除业务数据。
     * @param userId userId 参数
     * @param id id 参数
     */
    void delete(Long userId, Long id);


    /**
     * 设置相关数据。
     * @param userId userId 参数
     * @param id id 参数
     */
    void setDefault(Long userId, Long id);
}
