# 项目级错误索引

| 错误关键词 | 文档链接 | 简要描述 |
|------------|----------|----------|
| 平台端 me/商品审核接口缺失 + 代理/精度问题 | [平台端接口缺失（me与商品审核详情端点）.md](平台端接口缺失（me与商品审核详情端点）.md) | GET /me、GET /spus/{id} 缺失；vite 代理绕过网关；雪花 ID Number() 精度丢失 |
| 平台端角色管理 Redis 连接拒绝 | [平台端角色管理接口500（Redis未配置）.md](平台端角色管理接口500（Redis未配置）.md) | platform-service 缺少 Redis 配置，listRoles 触发连接异常 |
| 网关 JwtVerifier 启动失败 | [网关启动失败（JwtVerifier占位符无法解析）.md](网关启动失败（JwtVerifier占位符无法解析）.md) | gateway 不依赖 minimall-common → DotenvConfig 不在 classpath → .env 未加载 |
| merchant auth /me 404 | [路由未匹配（静态资源未找到）.md](路由未匹配（静态资源未找到）.md) | MerchantAuthControllerV1 缺 GET /me，404 被当静态资源处理 |
| 敏感字段解密失败（AES-256-GCM） | [数据解密失败（EncryptedStringTypeHandler敏感字段解密）.md](数据解密失败（EncryptedStringTypeHandler敏感字段解密）.md) | EncryptedStringTypeHandler 读取 receiver_name 时解密失败；根因：密钥不一致 / 明文数据 / 密文截断 |
| 统一数据库合并 | [统一数据库合并.md](统一数据库合并.md) | 11 个分库合并为 minimall_db；49 个 Flyway 迁移压缩为单一 V1__init_minimall.sql；9 份服务级 schema.sql 集中到 flyway-core 共享；H2 索引跨表重名需表前缀命名 |
