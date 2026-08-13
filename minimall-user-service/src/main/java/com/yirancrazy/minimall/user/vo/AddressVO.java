package com.yirancrazy.minimall.user.vo;

import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.user.entity.AddressPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货地址VO，用于Controller边界输出，隐藏内部字段
 * @Version: 1.0
 * @DateTime: 2026/08/04
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
     * 将 AddressPO 转换为 AddressVO。
     * @param po 收货地址持久化对象
     * @return 收货地址VO
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
