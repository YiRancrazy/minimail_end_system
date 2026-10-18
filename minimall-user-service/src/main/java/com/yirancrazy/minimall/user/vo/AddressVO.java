package com.yirancrazy.minimall.user.vo;

import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.user.entity.AddressPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AddressVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
public class AddressVO {

    private Long id;
    private Long userId;
    private String receiver;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private Boolean isDefault;
    private LocalDateTime createTime;


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @return 处理结果
     */
    public static AddressVO from(AddressPO po) {
        AddressVO vo = new AddressVO();
        vo.setId(po.getId());
        vo.setUserId(po.getUserId());
        vo.setReceiver(po.getReceiverName());
        vo.setPhone(po.getReceiverPhone());
        vo.setProvince(po.getProvince());
        vo.setCity(po.getCity());
        vo.setDistrict(po.getDistrict());
        vo.setDetail(po.getDetailAddress());
        vo.setIsDefault(po.getIsDefault() != null && po.getIsDefault() == 1);
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
