package com.yirancrazy.minimall.user.vo;

import com.yirancrazy.minimall.user.entity.AddressPo;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收货地址接口视图，将持久化密文字段以外的地址信息及默认标记转换为前端可展示模型.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class AddressVo {

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
     * 将收货地址持久化对象转换为接口视图。
     *
     * <p>地址字段按名称映射；isDefault 仅当数据库值为 1 时转换为 true，NULL 或其他值均视为 false。
     *
     * @param po 收货地址持久化对象
     * @return 转换后的收货地址视图
     */
    public static AddressVo from(AddressPo po) {
        AddressVo vo = new AddressVo();
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
