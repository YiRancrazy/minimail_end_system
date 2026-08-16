package com.yirancrazy.minimall.common.security;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MyBatis字符串加密类型处理器，写入时自动AES-256-GCM加密，读取时自动解密。通过EncryptionContext获取密钥。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@MappedTypes(String.class)
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
        throws SQLException {
        byte[] key = EncryptionContext.getKey();
        String encrypted = AesEncryptor.encrypt(parameter, key);
        ps.setString(i, encrypted);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return decryptSafely(value);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return decryptSafely(value);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return decryptSafely(value);
    }

    // 解密失败必须显式失败，禁止静默返回密文（密文会被当明文使用/展示，掩盖加密列截断或密钥变更问题）
    private String decryptSafely(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        try {
            byte[] key = EncryptionContext.getKey();
            return AesEncryptor.decrypt(value, key);
        }
        catch (Exception e) {
            throw new BizException(CommonCode.SYS_ERROR, "DECRYPT_FAIL", "敏感字段解密失败");
        }
    }
}
