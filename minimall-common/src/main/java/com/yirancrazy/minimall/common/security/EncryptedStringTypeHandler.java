package com.yirancrazy.minimall.common.security;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MyBatis 字符串列加解密处理器，写入前使用 AES-GCM 加密，读取后解密并返回明文。
 * 解密失败时抛出 BizException 暴露数据或密钥问题，禁止静默把密文当明文返回。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@MappedTypes(String.class)
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    /**
     * 使用当前加密上下文把非空字符串参数加密后写入 PreparedStatement。
     *
     * @param ps 待写入的 PreparedStatement
     * @param i 参数索引
     * @param parameter 明文字符串
     * @param jdbcType JDBC 类型
     * @throws SQLException 写入数据库参数失败时抛出
     */
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        byte[] key = EncryptionContext.getKey();
        String encrypted = AesEncryptor.encrypt(parameter, key);
        ps.setString(i, encrypted);
    }

    /**
     * 按列名读取密文并解密，null 和空字符串原样返回。
     *
     * @param rs 查询结果集
     * @param columnName 加密列名
     * @return 解密后的明文或原始空值
     * @throws SQLException 读取列失败时抛出
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return decryptSafely(value);
    }

    /**
     * 按列索引读取密文并解密，null 和空字符串原样返回。
     *
     * @param rs 查询结果集
     * @param columnIndex 加密列索引
     * @return 解密后的明文或原始空值
     * @throws SQLException 读取列失败时抛出
     */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return decryptSafely(value);
    }

    /**
     * 从存储过程结果集按列索引读取密文并解密。
     *
     * @param cs CallableStatement
     * @param columnIndex 加密列索引
     * @return 解密后的明文或原始空值
     * @throws SQLException 读取列失败时抛出
     */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return decryptSafely(value);
    }

    // 解密失败必须显式失败，禁止静默返回密文（密文会被当明文使用/展示，掩盖加密列截断或密钥变更问题）
    /**
     * 对非空密文执行解密；失败时记录原始异常类型和密文长度，并包装为带诊断别名的业务异常。
     *
     * @param value 数据库中的密文
     * @return 明文或原始空值
     * @throws BizException 密文非法、密钥不匹配或解密失败时抛出
     */
    private String decryptSafely(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        try {
            byte[] key = EncryptionContext.getKey();
            return AesEncryptor.decrypt(value, key);
        } catch (Exception e) {
            // 诊断日志：保留原始异常类型和密文长度，便于区分密钥不匹配(AEADBadTagException)
            // vs 明文数据(IllegalArgumentException) vs 密文截断(IllegalBlockSizeException)
            log.error(
                    "解密失败，valueLength={}，exceptionType={}", value.length(), e.getClass().getSimpleName(), e);
            throw new BizException(CommonCode.SYS_ERROR, "DECRYPT_FAIL", "敏感字段解密失败", e);
        }
    }
}
