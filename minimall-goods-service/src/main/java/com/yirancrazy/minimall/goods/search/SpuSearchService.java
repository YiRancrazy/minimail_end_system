package com.yirancrazy.minimall.goods.search;

import java.util.List;
import com.yirancrazy.minimall.goods.dto.SpuSearchDTO;
import com.yirancrazy.minimall.goods.vo.SpuSearchVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuSearchService 服务接口，定义相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface SpuSearchService {


    /**
     * 执行 search 方法。
     * @param dto dto 参数
     * @return 结果列表
     */
    List<SpuSearchVO> search(SpuSearchDTO dto);


    /**
     * 执行 sync 方法。
     * @param document document 参数
     */
    void sync(SpuDocument document);


    /**
     * 删除业务数据。
     * @param id id 参数
     */
    void deleteById(Long id);
}
