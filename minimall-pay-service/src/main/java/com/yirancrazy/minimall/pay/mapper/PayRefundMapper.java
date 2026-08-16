package com.yirancrazy.minimall.pay.mapper;

import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.pay.entity.PayRefundPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MyBatis Mapper 接口，提供数据库映射操作。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Mapper
public interface PayRefundMapper extends BaseMapper<PayRefundPO> {

    /**
     * 统计指定支付单已占用退款金额（在途待发起 PENDING + 已成功 SUCCESS），用于累计退款上限校验，
     * 失败/关闭的退款记录不计入，避免并发部分退款累计超退。
     * @param paymentNo 支付单号
     * @return 已占用退款金额；无记录返回 0
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_pay_refund "
        + "WHERE payment_no = #{paymentNo} AND status IN (0, 1) AND is_deleted = 0")
    BigDecimal sumCommittedAmount(@Param("paymentNo") String paymentNo);
}