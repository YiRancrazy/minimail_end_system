package com.yirancrazy.minimall.common.constant.jwt;

/**
 * @Author: YiRanCrazy@gmail.com
 * @Description: JWT 令牌类型常量集合，用于区分短期访问令牌和长期刷新令牌。
 * 该类只保存协议值，不负责签发、解析或吊销令牌。
 * @Version: 1.0
 * @DateTime: 2026/10/31 11:12
 */
public final class JwtType {
    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";
}
