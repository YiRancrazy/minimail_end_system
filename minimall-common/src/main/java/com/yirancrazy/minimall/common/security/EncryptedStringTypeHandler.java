package com.yirancrazy.minimall.common.security;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: EncryptedStringTypeHandler 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@MappedTypes(String.class)
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    /**
     * 设置相关数据。
     * @param ps ps 参数
     * @param i i 参数
     * @param parameter parameter 参数
     * @param jdbcType jdbcType 参数
     * @throws SQLException 处理失败时抛出
     */
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
        throws SQLException {
        byte[] key = EncryptionContext.getKey();
        String encrypted = AesEncryptor.encrypt(parameter, key);
        ps.setString(i, encrypted);
    }

    /**
     * 获取相关数据。
     * @param rs rs 参数
     * @param columnName columnName 参数
     * @return 处理结果
     * @throws SQLException 处理失败时抛出
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return decryptSafely(value);
    }

    /**
     * 获取相关数据。
     * @param rs rs 参数
     * @param columnIndex columnIndex 参数
     * @return 处理结果
     * @throws SQLException 处理失败时抛出
     */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return decryptSafely(value);
    }

    /**
     * 获取相关数据。
     * @param cs cs 参数
     * @param columnIndex columnIndex 参数
     * @return 处理结果
     * @throws SQLException 处理失败时抛出
     */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return decryptSafely(value);
    }

    // 解密失败必须显式失败，禁止静默返回密文（密文会被当明文使用/展示，掩盖加密列截断或密钥变更问题）
    /**
     * 处理decryptSafely。
     * @param value 值
     * @return 处理结果
     * @throws BizException 业务校验失败时抛出
     */
    private String decryptSafely(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        try {
            byte[] key = EncryptionContext.getKey();
            return AesEncryptor.decrypt(value, key);
        }
        catch (Exception e) {
            // 诊断日志：保留原始异常类型和密文长度，便于区分密钥不匹配(AEADBadTagException)
            // vs 明文数据(IllegalArgumentException) vs 密文截断(IllegalBlockSizeException)
            log.error("解密失败，valueLength={}，exceptionType={}",
                value.length(), e.getClass().getSimpleName(), e);
            throw new BizException(CommonCode.SYS_ERROR, "DECRYPT_FAIL", "敏感字段解密失败", e);
        }
    }
}
