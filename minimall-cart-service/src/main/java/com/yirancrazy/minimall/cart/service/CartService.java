package com.yirancrazy.minimall.cart.service;

import java.util.List;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartUpdateDTO;
import com.yirancrazy.minimall.cart.vo.CartItemVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface CartService {

    /**
     * 查询列表数据。
     * @param userId userId 参数
     * @return 结果列表
     */
    List<CartItemVO> listByUser(Long userId);


    /**
     * 执行 add 方法。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 业务数值
     */
    Long add(Long userId, CartItemAddDTO dto);


    /**
     * 删除业务数据。
     * @param id id 参数
     * @param userId userId 参数
     * @return 是否处理成功
     */
    boolean delete(Long id, Long userId);


    /**
     * 统计业务数据。
     * @param userId userId 参数
     * @return 业务数值
     */
    long countByUser(Long userId);


    /**
     * 更新业务数据。
     * @param id id 参数
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 是否处理成功
     */
    boolean update(Long id, Long userId, CartUpdateDTO dto);


    /**
     * 执行 selectAll 方法。
     * @param userId userId 参数
     * @param selected selected 参数
     * @return 是否处理成功
     */
    boolean selectAll(Long userId, Integer selected);


    /**
     * 执行 clear 方法。
     * @param userId userId 参数
     * @return 是否处理成功
     */
    boolean clear(Long userId);


    /**
     * 执行 moveToFavorite 方法。
     * @param userId userId 参数
     * @param itemIds itemIds 参数
     */
    void moveToFavorite(Long userId, List<Long> itemIds);
}
