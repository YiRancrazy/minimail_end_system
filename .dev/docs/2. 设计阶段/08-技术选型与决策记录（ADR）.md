# 技术选型与决策记录（ADR）

## 文档信息

| 字段 | 内容 |
|------|------|
| 文档名称 | 技术选型与决策记录（ADR） |
| 文档版本 | V1.0 |$
| 所属阶段 | 设计阶段 |
| 文档状态 | 已发布 |
| 创建人 | yirancrazy@gmail.com |
| 创建日期 | 2026-07-27 |
| 最后更新 | 2026-07-28 |[\$]

## 文档目的

> 记录项目中所有重要的技术决策（采用/放弃某项技术、关键架构选型），保证决策可追溯、上下文不丢失。

## 适用范围

- **适用对象**：架构师、研发负责人
- **适用场景**：技术选型评审、架构调整、新成员了解历史背景
- **不适用范围**：Bug 修复、纯配置变更

## 详细内容

### 1. ADR 模板（Michael Nygard 风格）

```markdown
# ADR-NNN: <决策标题>

## 状态
提议 / 已接受 / 已废弃 / 已取代

## 背景
当前面临什么问题、约束、需求。

## 决策
我们决定做什么。

## 后果
- 正面：带来的好处
- 负面：付出的代价、风险
- 中性：需要注意的事项

## 替代方案
考虑过但未采纳的方案及其原因。

## 参与人
- 提案人：
- 评审人：
- 决策日期：
```

### 2. ADR 列表

| 编号 | 标题 | 状态 | 日期 |
|------|------|------|------|
| ADR-001 | 采用 Spring Boot 3.2.x + Java 17 LTS 作为基础框架 | 已接受 | 2026-07-27 |
| ADR-002 | 选用 MyBatis-Plus 3.5.7 作为 ORM | 已接受 | 2026-07-27 |
| ADR-003 | 引入 Nacos 2.3+ 作为配置/注册中心 | 已接受 | 2026-07-27 |
| ADR-004 | 消息队列选用 RocketMQ 5.x | 已接受 | 2026-07-27 |
| ADR-005 | 缓存选用 Redis 7.2.4（锁定patch）Cluster | 已接受 | 2026-07-27 |
| ADR-006 | 分布式 ID 选用雪花算法 | 已接受 | 2026-07-27 |
| ADR-007 | 服务间通信采用 OpenFeign | 已接受 | 2026-07-27 |
| ADR-008 | 全局异常处理采用 @RestControllerAdvice | 已接受 | 2026-07-27 |
| ADR-009 | 统一返回 Result\<T\> 包装 | 已接受 | 2026-07-27 |
| ADR-010 | 日志框架选用 Logback + MDC | 已接受 | 2026-07-27 |
| ADR-011 | 构建工具选用 Maven 3.8.6 | 已接受 | 2026-07-27 |
| ADR-012 | 微服务框架选用 Spring Cloud Alibaba 2023.0.1.x | 已接受 | 2026-07-27 |
| ADR-013 | Web 框架选用 Spring Web MVC | 已接受 | 2026-07-27 |
| ADR-014 | API 网关选用 Spring Cloud Gateway 4.x | 已接受 | 2026-07-27 |
| ADR-015 | 分布式事务选用 Seata 2.x AT 模式 | 已接受 | 2026-07-27 |
| ADR-016 | 数据库选用 MySQL 8.0 LTS | 已接受 | 2026-07-27 |
| ADR-017 | 搜索引擎选用 Elasticsearch 8.11.x | 已接受 | 2026-07-27 |
| ADR-018 | 对象存储统一使用 MinIO | 已接受 | 2026-07-27 |
| ADR-019 | 链路追踪选用 Apache SkyWalking 9.x | 已接受 | 2026-07-27 |
| ADR-020 | 指标监控选用 Prometheus + Grafana | 已接受 | 2026-07-27 |
| ADR-021 | 日志采集选用 Loki + Promtail | 已接受 | 2026-07-27 |
| ADR-022 | 容器部署选用 Kubernetes 1.29+ | 已接受 | 2026-07-27 |
| ADR-023 | CI/CD 选用 GitLab CI / GitHub Actions | 已接受 | 2026-07-27 |
| ADR-024 | 镜像仓库选用 Harbor | 已接受 | 2026-07-27 |
| ADR-025 | 第三方支付接入支付宝开放平台 | 已接受 | 2026-07-27 |
| ADR-026 | 排除 Dubbo + Triple | 已接受 | 2026-07-27 |
| ADR-027 | 排除 Spring Cloud Sleuth | 已接受 | 2026-07-27 |
| ADR-028 | 排除 Eureka | 已接受 | 2026-07-27 |
| ADR-029 | 排除 OpenFeign 作为异步/高并发调用方式 | 已接受 | 2026-07-27 |
| ADR-029.1 | 异步调用方案 | 已接受 | 2026-07-27 |
| ADR-030 | 选用 XXL-JOB 3.0.0+ 作为分布式定时任务调度中心 | 已接受 | 2026-07-27 |
| ADR-031 | 容器化非功能需求 | 已接受 | 2026-07-28 |
| ADR-032 | 选用 GitHub Actions 作为 CI/CD 平台 | 已接受 | 2026-07-28 |
| ADR-033 | 选用 HashiCorp Vault 作为 KMS | 已接受 | 2026-07-28 |
| ADR-034 | 选用 SSE 实现实时推送 | 已接受 | 2026-07-28 |
| ADR-035 | 选用 阿里云SMS+邮件推送 | 已接受 | 2026-07-28 |
| ADR-036 | 灰度发布方案 — Gateway+Nacos元数据路由 | 已接受 | 2026-07-28 |
| ADR-037 | 首期不做数据库读写分离 | 已接受 | 2026-07-28 |
| ADR-038 | 数据库版本管理选用 Flyway | 已接受 | 2026-07-28 |
| ADR-039 | 认证机制采用 Access+Refresh 双token | 已接受 | 2026-07-28 |
| ADR-040 | 生产日志采用 JSON 结构化格式 | 已接受 | 2026-07-28 |
| ADR-041 | API 版本策略采用 URL 路径版本 | 已接受 | 2026-07-28 |
| ADR-042 | Maven 多模块采用三层结构 | 已接受 | 2026-07-28 |
| ADR-043 | 本地开发采用 Docker Compose 一键启动 | 已接受 | 2026-07-28 |
| ADR-044 | 代码生成策略为手写全部 | 已接受 | 2026-07-28 |
| ADR-045 | 枚举持久化采用 code(int) 存储 | 已接受 | 2026-07-28 |
| ADR-046 | Feign 异常采用 ErrorDecoder 直接传播 | 已接受 | 2026-07-28 |
| ADR-047 | 分布式锁选用 Redisson 可重入锁 | 已接受 | 2026-07-28 |
| ADR-048 | 本地缓存选用 Caffeine | 已接受 | 2026-07-28 |
| ADR-049 | 接口幂等性采用 Token 机制 | 已接受 | 2026-07-28 |
| ADR-050 | API 文档采用 SpringDoc OpenAPI 3 + Knife4j UI | 已接受 | 2026-07-28 |
| ADR-051 | 业务异常码按服务分段 | 已接受 | 2026-07-28 |
| ADR-052 | 消息可靠性采用 RocketMQ 事务消息 | 已接受 | 2026-07-28 |
| ADR-053 | 消息顺序性采用 RocketMQ 顺序消息 | 已接受 | 2026-07-28 |
| ADR-054 | 首期不分库分表，预留分片键 | 已接受 | 2026-07-28 |
| ADR-055 | 缓存一致性采用 Cache-Aside + 延迟双删 | 已接受 | 2026-07-28 |
| ADR-056 | 敏感数据采用 AES-256 字段级加密 | 已接受 | 2026-07-28 |
| ADR-057 | 慢 SQL 监控阈值为 1 秒 | 已接受 | 2026-07-28 |
| ADR-058 | 日志保留策略为 30 天热数据 + 90 天归档 | 已接受 | 2026-07-28 |
| ADR-059 | 告警渠道仅使用邮件 | 已接受 | 2026-07-28 |
| ADR-060 | 服务健康检查采用 Actuator + Nacos 心跳 | 已接受 | 2026-07-28 |
| ADR-061 | Druid 连接池参数：初始 5 + 最大 20 + 超时 60 秒 | 已接受 | 2026-07-28 |
| ADR-062 | Feign 超时配置：连接 5 秒 + 读取 10 秒 + 重试 1 次 | 已接受 | 2026-07-28 |

### 3. ADR 详细内容

> ADR编号规则：主编号3位递增（ADR-001, ADR-002, ...），子决策用小数点（ADR-002.1, ADR-029.1）。子编号表示对主决策的补充或修正，不改变主决策状态。

#### ADR-001：采用 Spring Boot 3.2.x + Java 17 LTS

**状态**：已接受

**背景**：
- 项目需快速搭建，团队熟悉 Spring 生态
- Java 17 LTS 已为团队主流 LTS 版本（虚拟线程需 JDK 21，本期不启用）
- 微服务场景下需集成配置中心、注册中心、链路追踪

**决策**：
采用 Spring Boot 3.2.x + Spring Cloud 2023.0.1.x，Java 17 LTS。

**后果**：
- ✅ 生态成熟，文档丰富，招聘容易
- ✅ 与 Nacos/Sentinel/SkyWalking 集成简单
- ⚠️ 需注意 Jakarta EE 包名变更（javax.* → jakarta.*）
- ⚠️ 第三方库需升级到兼容版本
- ⚠️ Spring Cloud 2023.0.x (Leyton) 于2025年7月1日进入仅商业支持阶段，开源OSS安全补丁已停止。风险自担，需在项目风险管理文档中登记。建议：锁定SC 2023.0.1.0 + SCA 2023.0.1.0版本，禁止小版本自动升级；安全漏洞需手动patch或评估升级至SC 2025.0.x (Northfields)。
- ⚠️ 锁定 Spring Boot 3.2.x，禁止升级至 3.3.x+。原因：MyBatis-Plus 3.5.x 在 Spring Boot 3.3.x+ 上存在 `factoryBeanObjectType` 兼容性 Bug，需等 MP 3.6.0 发布后再评估。

**替代方案**：
- Quarkus：性能好但生态不成熟
- Helidon：团队无经验
- 原生 Spring 4.x：维护成本高

**参与人**：
- 提案人：架构师
- 评审人：3 名核心研发
- 决策日期：2026-07-27

---

#### ADR-002：选用 MyBatis-Plus 3.5.7（推荐）/3.5.17（最新）

**状态**：已接受

**背景**：
- 团队有 MyBatis 经验
- 业务以复杂动态 SQL 为主

**决策**：
MyBatis-Plus 3.5.7（推荐）/3.5.17（最新） + 分页插件 + 乐观锁插件 + 自动填充。

**后果**：
- ✅ 零 SQL 简单 CRUD
- ✅ 动态 SQL 仍可手写，灵活性高
- ⚠️ 多表关联需手写 resultMap
- ⚠️ 分页插件需注意 withCount
- ⚠️ Spring Boot 3.x 必须使用 `mybatis-plus-spring-boot3-starter`（非 `mybatis-plus-boot-starter`），否则会报 `factoryBeanObjectType` 错误。禁止升级至 Spring Boot 3.3.x+，MP 3.5.x 在 3.3.x 上存在不兼容 Bug，需等 MP 3.6.0+。
- ⚠️ 3.5.9+ 版本需额外引入 `mybatis-plus-jsqlparser`（JDK11+）模块以支持分页等插件。

**替代方案**：
- JPA/Hibernate：动态 SQL 难，复杂场景受限
- MyBatis 裸用：CRUD 工作量大
- BeetlSQL：团队无经验

**参与人**：
- 提案人：架构师
- 评审人：2 名后端研发
- 决策日期：2026-07-27

---

#### ADR-002.1：选用 Druid 1.2.21（锁定）作为数据库连接池

**状态**：已接受

**背景**：
- Spring Boot 3.2.x 默认连接池为 HikariCP，性能优异但缺少 SQL 监控和防火墙能力
- 生产环境需要慢 SQL 日志、连接泄漏检测、SQL 防火墙（防注入）等运维能力
- 团队已有 Druid 使用经验，Druid 1.2.x 兼容 Spring Boot 3

**决策**：
选用 Druid 1.2.21（锁定）替代 HikariCP。必须开启：StatFilter（慢SQL≥1s，见ADR-057）、ConfigFilter（密码加密）、StatViewServlet（监控页面）。

**后果**：
- ✅ 内置 SQL 监控 + 慢 SQL 日志 + 防火墙，无需额外中间件
- ✅ StatViewServlet 提供可视化监控页面
- ❌ 性能略低于 HikariCP（约 5-10%），但 IO 密集型场景差异可忽略
- ❌ 配置项多于 HikariCP
- ⚠️ Spring Boot 3.x 必须使用 `druid-spring-boot-3-starter`（非 `druid-spring-boot-starter`）。1.2.18 版本缺少 `AutoConfiguration.imports` 文件导致自动装配失败，1.2.22 版本 yaml 配置读取有问题，因此锁定 1.2.21。

**排除项**：
- HikariCP：无 SQL 监控能力，需额外接入 p6spy 或 Prometheus exporter

**Druid 与 SkyWalking 分工**：
- Druid StatFilter：连接池管理 + 慢 SQL 日志（≥1s，见ADR-057）写入业务日志文件（ops 视角）
- SkyWalking：链路级慢 SQL 追踪 + 调用拓扑 + P99 指标（dev 视角）
- 两者不冲突：Druid 关注"单条 SQL 慢"，SW 关注"链路中 SQL 慢"

**回退方案**：
- 如 Druid 连接池性能不满足，移除 Druid StatFilter，切换 HikariCP + p6spy 实现慢 SQL 日志

---

#### ADR-003：引入 Nacos 2.3+ 作为配置/注册中心

**状态**：已接受

**背景**：
- 微服务需注册中心和配置中心，两者独立部署运维成本高
- 团队已有 Nacos 使用经验，Spring Cloud Alibaba 原生支持

**决策**：
Nacos 2.3.2 同时承担注册中心与配置中心双角色（对齐 SCA 2023.0.1.0）。

**后果**：
- ✅ 双角色合一，运维组件减少
- ✅ 服务健康检查与配置热更新一体化
- ✅ Spring Cloud Alibaba 原生适配，零额外集成
- ⚠️ Nacos Server 自身需高可用部署（3 节点集群）
- ⚠️ 配置中心敏感信息需配合 KMS 加密，禁止明文存储
- ⚠️ Namespace 按环境隔离设计：

| namespace | 用途 | DataId 示例 |
|-----------|------|-------------|
| dev | 开发环境 | minimall-order-service.yaml |
| test | 测试环境 | minimall-order-service.yaml |
| staging | 预发环境 | minimall-order-service.yaml |
| prod | 生产环境 | minimall-order-service.yaml |

  同名 DataId 通过 namespace 隔离，禁止跨 namespace 引用。

- ⚠️ 启用 Nacos 2.3+ 内置配置加密（AES-256），数据库密码、Redis 密码、支付密钥等敏感配置必须加密存储。加密 key 由 Nacos 服务端管理，应用端通过 ConfigFilter 自动解密。

**替代方案**：
- Eureka + Spring Cloud Config：Eureka 已停止积极维护（见 ADR-028）
- Consul：团队无经验，KV 存储性能弱于 Nacos
- ZooKeeper：重且无配置热更新能力

**参与人**：
- 提案人：架构师
- 评审人：运维负责人、2 名后端研发
- 决策日期：2026-07-27

---

#### ADR-004：消息队列选用 RocketMQ 5.x

**状态**：已接受

**背景**：
- 订单创建与支付回调需事务消息保证最终一致性
- 顺序消息用于库存扣减等有序场景
- Spring Cloud Alibaba 原生集成

**决策**：
RocketMQ 5.1.4 作为核心消息队列，利用事务消息 + 顺序消息能力。使用 `rocketmq-spring-boot-starter` 2.3.1+ 兼容 Spring Boot 3.2，5.x Broker 与 4.9.x Client 协议兼容，客户端可平滑升级（对齐 SCA 2023.0.1.0）。

**后果**：
- ✅ 事务消息原生支持，无需本地消息表即可实现跨服务最终一致
- ✅ 顺序消息支持，库存扣减等场景可保证有序
- ✅ Spring Cloud Alibaba 原生适配
- ✅ 5.x Broker 新架构（Proxy 模式）运维更简洁，支持 gRPC 协议
- ⚠️ 运维复杂度较高（Namesrv + Broker 双主双从）
- ⚠️ 事务消息回查需业务侧实现 LocalTransactionChecker

**替代方案**：
- Kafka：事务消息支持弱，顺序消息仅分区内有序
- RabbitMQ：事务消息无原生支持，需 confirm + 死信模拟
- Pulsar：团队无经验，运维成本高

**参与人**：
- 提案人：架构师
- 评审人：3 名核心研发
- 决策日期：2026-07-27

---

#### ADR-005：缓存选用 Redis 7.2.4（锁定patch）Cluster

**状态**：已接受

**背景**：
- 热点数据（商品、库存）需亚毫秒级读取
- 分布式锁用于互斥性资源（提现审核等）
- 计数类场景（购物车、库存预警）需原子操作

**决策**：
Redis 7.2.4（锁定patch）Cluster 模式，6 节点（3 主 3 从），配合 Redisson 分布式锁。Docker 镜像必须使用 `redis:7.2.4`，禁止 `:7.2` 或 `:latest` 浮动标签。升级前必须验证许可证未变更。

**后果**：
- ✅ Cluster 模式水平扩展，单节点瓶颈可加片
- ✅ Stream / Function 等新特性可按需启用
- ✅ Redisson 锁语义丰富（可重入、读写锁）
- ✅ 锁定 patch 版本避免小版本间行为差异
- ⚠️ Cluster 模式下 mget/mset 跨槽需 hash tag
- ⚠️ Redis 不可用时需本地缓存兜底，业务需实现降级
- ⚠️ Redis 7.4+ 采用 RSALv2/SSPL 许可证，存在合规风险。本期锁定 7.2.4（BSD 许可），禁止升级至 7.4+。如需新特性，评估 Valkey 替代

**替代方案**：
- Memcached：无持久化、无数据结构、无发布订阅
- 单机 Redis：无高可用
- Sentinel 模式：写入仍单点

**参与人**：
- 提案人：架构师
- 评审人：运维负责人、2 名后端研发
- 决策日期：2026-07-27

---

#### ADR-006：分布式 ID 选用雪花算法

**状态**：已接受

**背景**：
- 主键需全局唯一且趋势递增
- 分库分表后自增 ID 会冲突
- UUID 无序导致 B+ 树页分裂性能差

**决策**：
独立雪花 ID 服务（id-service）生成分布式 ID，基于 Snowflake 算法 + workerId 由 Nacos 动态分配。

**后果**：
- ✅ 趋势递增，对 B+ 树索引友好
- ✅ 纯数字 64bit，存储紧凑
- ✅ 跨服务唯一 ID 统一由 id-service 生成，避免各服务 workerId 冲突
- ⚠️ 时钟回拨需防护（id-service 内置时钟回拨检测与等待策略）
- ⚠️ id-service 需高可用部署，单点故障将影响全局 ID 生成
- ⚠️ MP 内置雪花算法仅用于单服务内部非分布式 ID 生成，跨服务唯一 ID 统一由 id-service 生成。id-service 基于 Snowflake 算法 + workerId 由 Nacos 动态分配

**替代方案**：
- UUID：无序，索引性能差
- 号段模式（Leaf）：依赖 DB，多一层依赖
- 数据库自增：分库分表后冲突

**参与人**：
- 提案人：架构师
- 评审人：2 名后端研发
- 决策日期：2026-07-27

---

#### ADR-007：服务间通信采用 OpenFeign

**状态**：已接受

**背景**：
- 微服务间需同步调用（如 order → stock 锁库存）
- 团队习惯声明式 HTTP 客户端
- 需与 Sentinel 限流/熔断集成

**决策**：
Spring Cloud OpenFeign 作为服务间同步调用客户端，配合 Sentinel 1.8.6 做熔断降级。

**后果**：
- ✅ 声明式接口，开发效率高
- ✅ 与 Sentinel 集成简单
- ✅ 负载均衡由 Spring Cloud LoadBalancer 提供
- ⚠️ 同步阻塞 IO，线程池耗尽风险（见 ADR-029 后续评估）
- ⚠️ 超时/重试需逐接口配置，默认值不合理

**替代方案**：
- Dubbo + Triple：RPC 风格与 REST 不统一（见 ADR-026）
- WebClient：响应式编程模型团队不熟悉
- RestTemplate：无声明式能力，代码冗余

**参与人**：
- 提案人：架构师
- 评审人：3 名核心研发
- 决策日期：2026-07-27

---

#### ADR-008：全局异常处理采用 @RestControllerAdvice

**状态**：已接受

**背景**：
- 各 Controller 异常处理逻辑重复，try-catch 散落各处
- 异常响应格式不统一，前端需适配多种错误结构
- 部分 Controller 吞掉异常导致问题难定位

**决策**：
`@RestControllerAdvice` + `@ExceptionHandler` 统一拦截，BizException → Result.fail，未知异常 → Result.fail + 告警。

**后果**：
- ✅ 异常处理集中，Controller 代码简洁
- ✅ 异常 → Result 统一响应，前端契约一致
- ✅ 未知异常自动告警，不遗漏
- ⚠️ 禁止 Controller 内 try-catch 吞掉异常
- ⚠️ 校验异常（MethodArgumentNotValidException）需单独处理

**替代方案**：
- 各 Controller 自行 try-catch：重复且易遗漏
- Filter 层拦截：无法获取 @ExceptionHandler 精细控制
- AOP @AfterThrowing：与 Spring MVC 异常处理机制冲突

**参与人**：
- 提案人：架构师
- 评审人：全栈负责人
- 决策日期：2026-07-27

---

#### ADR-009：统一返回 Result\<T\>

**状态**：已接受

**背景**：
- 各 Controller 返回类型不统一（有的返 VO、有的返 ResponseEntity、有的抛异常）
- 前后端约定不一致，前端需兼容多种结构

**决策**：
全局统一 `Result<T> { code, message, data }`，成功 `code="00000"`，业务错误 `1xxxx`，系统错误 `2xxxx`（5 位字符串，遵循《阿里规约 黄山版》）。
构造方法：`Result.success(data)` / `Result.fail(code, msg)`。
异常经 `@RestControllerAdvice` 统一转换为 `Result.fail`。

**后果**：
- ✅ 前后端契约统一
- ✅ 全局异常处理可读性高
- ⚠️ 已有非 Result 接口需迁移
- ⚠️ 文件下载/流式响应需特殊处理（直接返回 ResponseEntity）

**替代方案**：
- ResponseEntity\<T\>：HTTP 语义精确但前端解析不友好
- 各接口自定义响应结构：不统一，维护成本高
- GraphQL：过度设计，团队不熟悉

**参与人**：
- 提案人：架构师
- 评审人：全栈负责人
- 决策日期：2026-07-27

---

#### ADR-010：日志框架选用 Logback + MDC

**状态**：已接受

**背景**：
- Spring Boot 默认日志实现即为 Logback
- 需在日志中透传 traceId / userId / role 等上下文信息
- 日志采集需结构化输出（JSON 格式）

**决策**：
SLF4J + Logback，业务类使用 `@Slf4j`；MDC 透传 traceId / spanId / userId / role / merchantId；日志格式结构化 JSON 便于 Loki 采集。

**后果**：
- ✅ Spring Boot 零配置开箱即用
- ✅ MDC 上下文透传与 SkyWalking traceId 集成
- ✅ JSON 格式直接被 Loki + Promtail 采集
- ⚠️ 占位符写法 `log.info("user={}", userId)`，禁止字符串拼接
- ⚠️ 敏感信息脱敏：手机号/身份证/密码/银行卡禁止明文输出
- ⚠️ SkyWalking agent 自动将 TraceId 注入 Logback MDC（key=traceId），业务代码无需手动设置。日志模板：`%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId}] [%thread] %-5level %logger{36} - %msg%n`

**替代方案**：
- Log4j2：性能略优但配置复杂，Spring Boot 默认不走它
- java.util.logging：功能弱，不满足生产需求
- 自研日志框架：无必要

**参与人**：
- 提案人：架构师
- 评审人：2 名后端研发
- 决策日期：2026-07-27

---

#### ADR-011：构建工具选用 Maven 3.8.6

**状态**：已接受

**背景**：
- 公司统一构建工具为 Maven
- 多模块项目管理需成熟依赖管理机制
- CI/CD 流水线基于 Maven 标准化

**决策**：
Maven 3.8.6，配合 Maven Wrapper（`./mvnw`）确保版本一致性。

**后果**：
- ✅ 公司统一，新成员零学习成本
- ✅ 多模块 + BOM 依赖管理成熟
- ✅ 生态插件丰富（Surefire、JaCoCo、Jib 等）
- ⚠️ 构建速度慢于 Gradle（可通过 -T 并行构建缓解）
- ⚠️ POM 冗长，但可接受

**替代方案**：
- Gradle：构建速度快但公司未统一，迁移成本高
- Ant：无依赖管理，不适用微服务多模块
- Bazel：学习曲线极陡，团队无法接受

**参与人**：
- 提案人：架构师
- 评审人：DevOps 负责人
- 决策日期：2026-07-27

---

#### ADR-012：微服务框架选用 Spring Cloud Alibaba 2023.0.1.x

**状态**：已接受

**背景**：
- 需一站式集成 Nacos（注册/配置）、Sentinel（限流/熔断）、Seata（分布式事务）、RocketMQ（消息）
- 团队已有 Spring Cloud Netflix 经验，迁移到 Alibaba 成本低
- 版本需与 Spring Boot 3.2.x 对齐

**决策**：
Spring Cloud Alibaba 2023.0.1.0，对应 Spring Boot 3.2.x + Spring Cloud 2023.0.1.x。

**后果**：
- ✅ Nacos / Sentinel / Seata / RocketMQ 一站集成，版本兼容有保障
- ✅ 国内社区活跃，中文文档丰富
- ✅ Sentinel 限流/熔断与 Gateway / Feign 原生集成
- ⚠️ 版本对齐严格，升级需按官方版本对照表操作
- ⚠️ Seata Server 需独立部署运维

SCA 2023.0.1.0 组件版本对照：

| 组件 | 版本 | ADR |
|------|------|-----|
| Sentinel | 1.8.6 | ADR-007 / ADR-014 |
| Nacos | 2.3.2 | ADR-003 |
| RocketMQ | 5.1.4 | ADR-004 |
| Seata | 2.0.0 | ADR-015 |

**替代方案**：
- Spring Cloud Netflix（Hystrix/Zuul/Ribbon）：已停止维护
- Spring Cloud Tencent：社区规模小，生产验证不足
- 自组装（Spring Cloud + 手动集成各组件）：版本冲突风险高

**参与人**：
- 提案人：架构师
- 评审人：3 名核心研发
- 决策日期：2026-07-27

---

#### ADR-013：Web 框架选用 Spring Web MVC

**状态**：已接受

**背景**：
- 团队对 Spring MVC 编程模型熟悉
- 业务初期以 CRUD 为主，同步阻塞模型可满足
- 后续高并发场景可按需迁移 WebFlux 或使用 CompletableFuture+线程池优化 IO（见 ADR-029.1）

**决策**：
Spring Web MVC（同步阻塞模型），与 Spring Boot 3.2.x 默认一致。

**后果**：
- ✅ 团队零学习成本，开发效率最高
- ✅ 调试/排错简单，栈帧清晰
- ⚠️ 线程池模型下高并发需更多线程，内存开销大
- ⚠️ 后续若迁移 WebFlux 需全链路响应式改造，成本高

**替代方案**：
- Spring WebFlux：响应式模型性能好，但全链路改造成本高，团队不熟悉
- Vert.x：异步模型，团队无经验
- 虚拟线程：需 JDK 21，本期不适用；见 ADR-029

**参与人**：
- 提案人：架构师
- 评审人：2 名后端研发
- 决策日期：2026-07-27

---

#### ADR-014：API 网关选用 Spring Cloud Gateway 4.x

**状态**：已接受

**背景**：
- 外部请求需统一入口：路由、鉴权、限流、熔断
- 需响应式非阻塞模型支撑高并发长连接
- 需与 Sentinel 限流集成

**决策**：
Spring Cloud Gateway 4.x，基于 WebFlux 响应式模型，配合 Sentinel 1.8.6 限流/熔断 + JWT 验签。

**后果**：
- ✅ 响应式非阻塞，单实例支撑高并发连接
- ✅ 限流/熔断原生支持，与 Sentinel 集成简单
- ✅ 路由配置灵活（代码 / Nacos 动态路由）
- ⚠️ 基于 WebFlux，网关内不可用 Servlet API
- ⚠️ 自定义 Filter 需响应式编程，调试略复杂
- 网关安全增强：
  - 全局 AuthFilter：JWT 校验 + 黑名单检查（Redis SET 存储 `jti`，TTL = token 剩余有效期）
  - Sentinel 限流：按路由 ID 配置 QPS 阈值，超限返回 `429 Too Many Requests`
  - Sentinel 熔断：下游服务异常比例 > 50% 时熔断 5s，避免故障级联
  - 白名单（免鉴权路径）：`/api/v1/auth/login`、`/api/v1/auth/register` 等

  完整白名单：
  ```
  /api/v1/auth/login         — 登录
  /api/v1/auth/register      — 注册
  /api/v1/auth/sms-code      — 短信验证码
  /api/v1/auth/refresh-token — 刷新token
  /api/v1/auth/captcha       — 图形验证码
  /actuator/health           — K8s存活探针
  /druid/*                   — Druid监控页（仅内网）
  ```
  注册防刷：同 IP 1 分钟内最多 5 次注册请求，超限返回 429。

**替代方案**：
- Zuul 1.x：同步阻塞，已停止维护
- Zuul 2.x：异步但社区已停更
- Kong：功能强但需额外运维 + Lua 插件开发

**参与人**：
- 提案人：架构师
- 评审人：运维负责人、2 名后端研发
- 决策日期：2026-07-27

---

#### ADR-015：分布式事务选用 Seata 2.x AT 模式

**状态**：已接受

**背景**：
- 订单创建需同时锁定库存，跨 order_db / stock_db 需分布式事务
- 团队不熟悉 TCC/Saga 手动补偿编程模型
- 需与 Spring Cloud Alibaba 版本对齐

**决策**：
Seata 2.0.0 AT 模式，自动补偿；全局事务范围限定为「创建订单 + 锁定库存」，支付链路用事务消息保证最终一致（对齐 SCA 2023.0.1.0）。

**后果**：
- ✅ AT 模式零业务侵入，仅需加 `@GlobalTransactional` + `undo_log` 表
- ✅ 开发快，学习成本低
- ⚠️ 写放大：每条 SQL 产生一行 undo_log，高写场景需监控
- ⚠️ 全局锁粒度需注意，避免长事务持锁
- ⚠️ Seata Server 需独立高可用部署
- ⚠️ MQ + Seata 按场景分层规则：

| 场景 | 方案 | 示例 |
|------|------|------|
| 跨服务资金/库存操作 | Seata AT（强一致） | 下单→扣库存+创建订单 |
| 非资金跨服务通知 | MQ 最终一致 | 下单成功→发通知 |
| 第三方支付回调 | MQ 事务消息+本地消息表 | 支付回调→更新订单状态 |

  禁止同一链路混合使用 Seata AT 和 MQ 事务消息。
- ⚠️ Seata 1.x 与 2.x 不兼容，本期选定 2.x。Spring Boot 3.x 必须使用 Seata 2.x
- ⚠️ Seata 监控指标与阈值：

| 指标 | 阈值 | 告警级别 |
|------|------|----------|
| 分支事务写放大倍数 | > 2 倍 | P1 |
| 全局锁重试次数 | > 3 次/事务 | P1 |
| undo_log 表行数 | > 10 万行 | P2（需清理） |

**替代方案**：
- Seata TCC：性能好但需手动实现 Try/Confirm/Cancel 三接口，开发成本高
- Saga：长事务场景适用，但补偿逻辑复杂
- 本地消息表：无额外中间件，但需每张业务表配消息表，侵入性大

**参与人**：
- 提案人：架构师
- 评审人：3 名核心研发
- 决策日期：2026-07-27

---

#### ADR-016：数据库选用 MySQL 8.0 LTS

**状态**：已接受

**背景**：
- 团队熟悉 MySQL，运维经验丰富
- 需原生 JSON 类型（商品属性等半结构化数据）
- 需窗口函数支持对账/排行等复杂查询

**决策**：
MySQL 8.0 LTS，主从架构，初期同实例分 schema，后期按需独立实例。

**后果**：
- ✅ 团队熟悉，招聘/运维容易
- ✅ 原生 JSON / 窗口函数 / CTE 等新特性
- ✅ InnoDB 事务一致性保证资金正确性
- ⚠️ 单实例连接数上限需监控（见告警 `db_conn_pool_active`）
- ⚠️ 分库分表触发条件需提前规划（单表 > 1 亿行 / 写 QPS > 5k）
- ⚠️ MySQL 8.0 已于 2026 年 4 月 EOL，无官方安全更新。风险自担，需在项目风险管理文档中登记。建议监控 Percona/MySQL 社区分支动态，必要时迁移至 MySQL 8.4 LTS

**替代方案**：
- PostgreSQL：功能更强但团队经验不足，运维人才少
- TiDB：兼容 MySQL 协议但运维复杂度远高，首期不引入
- OceanBase：分布式数据库，同上
- MySQL 8.4 LTS：更长期的官方支持，但首期团队对 8.0 更熟悉，后续评估迁移

**参与人**：
- 提案人：架构师
- 评审人：DBA、2 名后端研发
- 决策日期：2026-07-27

---

#### ADR-017：搜索引擎选用 Elasticsearch 8.11.x

**状态**：已接受

**背景**：
- 商品搜索需多字段全文检索 + 复杂筛选 + 聚合
- MySQL LIKE 无法满足性能与功能需求
- 需与 Spring Data Elasticsearch 集成

**决策**：
Elasticsearch 8.11.x（锁定小版本），3 master + 3 data 节点，商品数据通过 MQ 异步索引同步。

**后果**：
- ✅ 多字段检索 + 模糊匹配 + 聚合能力强大
- ✅ 8.x 安全特性内置（Security 默认开启）
- ⚠️ ES 与 MySQL 漂移：搜索结果非强一致，标注"刚上架商品可能未立即出现"
- ⚠️ 运维成本高，需专职或托管
- ⚠️ 索引设计需提前规划，Mapping 变更需 Reindex
- ⚠️ ⚠️ Elasticsearch 小版本间存在 Breaking Changes（如 8.12 移除 type API），固定 8.11.x，升级大版本前必须跑 ES 兼容测试套件。
- ⚠️ ES 8.x 安全配置方案：
  - 启动时设置 `ELASTIC_PASSWORD` 环境变量初始化超级用户
  - 自动生成 HTTP 层 TLS 证书（`ca.crt` + `node.key` + `node.crt`）
  - 应用连接使用 `https` + 用户名密码认证
  - Dev 环境可用 `single-node` 模式 + 自签证书，降低本地开发门槛

**替代方案**：
- Solr：功能相当但社区萎缩，Spring 生态集成弱
- Meilisearch：轻量但缺少聚合、权限过滤等电商场景必需功能
- MySQL 全文索引：中文分词弱，性能差

**参与人**：
- 提案人：架构师
- 评审人：2 名后端研发
- 决策日期：2026-07-27

---

#### ADR-018：对象存储统一使用 MinIO

**状态**：已接受

**背景**：
- 商品图片、商家资质文件需对象存储
- 生产环境优先云厂商托管，开发/测试环境需本地兼容方案

**决策**：
统一使用 MinIO 作为对象存储，dev/prod 均部署 MinIO 集群，使用 S3 兼容 API。

**后果**：
- ✅ 统一技术栈，dev/prod 环境一致性高，排查问题无偏差
- ✅ MinIO S3 兼容，开发环境零成本
- ✅ 如未来需切换阿里云 OSS，通过 S3 协议适配层切换，业务代码零修改
- ⚠️ MinIO 生产使用需自行运维，需保证集群高可用
- ⚠️ MinIO 无 CDN 加速，需在前端接入 CDN 缓存静态资源

生产部署方案：
- 集群模式：3 节点 MinIO 集群（纠删码模式，可容忍 1 节点宕机）
- 持久化：每个节点挂载 hostPath PV（SSD，最小 100Gi），数据目录 `/minio/data`
- 备份策略：`mc mirror` 每日增量备份到异地 NFS，保留 7 天；全量备份每周一次
- 监控：MinIO Prometheus 指标（`s3_requests_total`、`disk_free_bytes`）

**替代方案**：
- 七牛云 Kodo：OSS 更主流
- AWS S3：国内延迟高，需备案
- NFS/本地磁盘：无分布式、无 CDN、不可水平扩展

**参与人**：
- 提案人：架构师
- 评审人：运维负责人
- 决策日期：2026-07-27

---

#### ADR-019：链路追踪选用 Apache SkyWalking 9.x

**状态**：已接受

**背景**：
- 微服务调用链需可视化，排查延迟/错误需 trace 上下文
- 需无侵入探针，不改动业务代码
- 国内 Spring Cloud Alibaba 生态首选

**决策**：
Apache SkyWalking 9.x，Java Agent 无侵入探针 + OpenTelemetry SDK 双轨。

**后果**：
- ✅ 无侵入探针，零业务代码改动
- ✅ 国内 SCA 生态首选，中文社区活跃
- ✅ 慢 SQL 排名、服务拓扑、告警一体化
- ⚠️ Agent 版本需与 OAP Server 版本对齐
- ⚠️ Agent 采样率需调优，全量采集影响性能
- ⚠️ SkyWalking trace 日志通过 grpc-exporter 写入 Loki，统一日志查询入口。TraceId 作为 Loki label，实现 Trace→Log 双向跳转。
- ⚠️ 指标分层：SkyWalking 负责服务级指标（P99/QPS/依赖拓扑/慢查询），Prometheus 负责基础设施指标（JVM/OS/MySQL/Redis/Nacos）。Grafana 看板分两行：服务行（SW 数据源）+ 基础设施行（Prometheus 数据源）。

**替代方案**：
- Jaeger：需侵入代码（OpenTelemetry SDK），无慢 SQL 排名
- Zipkin：功能弱，社区萎缩
- Sleuth：已停止维护（见 ADR-027）

**参与人**：
- 提案人：架构师
- 评审人：运维负责人、2 名后端研发
- 决策日期：2026-07-27

---

#### ADR-020：指标监控选用 Prometheus + Grafana

**状态**：已接受

**背景**：
- 需采集 JVM / DB 连接池 / Sentinel 限流 / QPS / P99 等指标
- K8s 环境下 Prometheus Operator 集成成本最低
- 告警需 AlertManager 对接钉钉/PagerDuty

**决策**：
Prometheus + Grafana + AlertManager，K8s 内 Prometheus Operator 自动发现。

**后果**：
- ✅ K8s 生态标配，Operator 自动服务发现
- ✅ Grafana 仪表盘生态丰富，开箱即用
- ✅ AlertManager 多渠道告警（钉钉/PagerDuty/邮件）
- ⚠️ Prometheus 单实例有 TSDB 容量上限，需按需分片或 Thanos
- ⚠️ 指标基数爆炸需控制（高基数 label 如 userId 禁用）

**替代方案**：
- InfluxDB：时序数据库但非 K8s 生态标配
- Datadog：SaaS 商业产品，数据出境合规风险
- Zabbix：传统监控，容器适配差

**参与人**：
- 提案人：运维负责人
- 评审人：架构师、SRE
- 决策日期：2026-07-27

---

#### ADR-021：日志采集选用 Loki + Promtail

**状态**：已接受

**背景**：
- 需结构化日志采集 + 与 Grafana 统一查询界面
- 日志量预估中等（日 50w 订单相关 + 200w 营销），无需重度全文索引
- 希望与 Prometheus/Grafana 同一技术栈

**决策**：
Loki + Promtail + Grafana，Loki 不建全文索引仅索引 label，资源占用低。

**后果**：
- ✅ 与 Grafana 一体化，日志 + 指标 + 链路统一查询
- ✅ 不建全文索引，存储成本远低于 ES
- ✅ Promtail 与 K8s DaemonSet 天然适配
- ⚠️ 全文搜索能力弱于 ES，复杂日志检索需 LogQL
- ⚠️ 大规模日志场景查询速度不如 ES

**替代方案**：
- ELK（Elasticsearch + Logstash + Kibana）：功能强大但存储成本高，且 ES 已用于搜索场景，职责重叠
- Fluentd + Elasticsearch：同上
- 云厂商日志服务（SLS/CLS）：商业产品，成本随量线性增长

**参与人**：
- 提案人：运维负责人
- 评审人：架构师
- 决策日期：2026-07-27

---

#### ADR-022：容器部署选用 Kubernetes 1.29+

**状态**：已接受

**背景**：
- 需滚动更新、HPA 自动扩缩容、Pod 自愈
- 微服务数量多（10+），手动运维不可行
- 团队已有 K8s 使用经验

**决策**：
Kubernetes 1.29+，3 master + 5 worker，业务服务 HPA 2-10 Pod。

**后果**：
- ✅ 滚动更新零停机，回滚秒级
- ✅ HPA 按 CPU/内存/自定义指标自动扩缩容
- ✅ Namespace 隔离 + NetworkPolicy 网络策略
- ⚠️ K8s 运维复杂度高，需专职 SRE 或托管服务
- ⚠️ 资源配额需提前规划（requests/limits）

**替代方案**：
- Docker Compose：单机编排，无弹性伸缩
- 裸机 + systemd：无法自动扩缩容、无法自愈
- 云厂商 ECS + 脚本：运维成本高，不可复现

**参与人**：
- 提案人：运维负责人
- 评审人：架构师、SRE
- 决策日期：2026-07-27

---

#### ADR-023：CI/CD 选用 GitLab CI / GitHub Actions

**状态**：已接受

**背景**：
- 代码托管平台决定 CI/CD 选型（GitLab 或 GitHub）
- 需支持多环境部署（dev/test/staging/prod）
- 需集成 SonarQube 代码质量扫描 + JaCoCo 覆盖率

**决策**：
根据代码托管平台选择 GitLab CI 或 GitHub Actions，流水线统一：lint → unit test → SonarQube → build → deploy。

**后果**：
- ✅ 与代码托管一体化，无需额外 CI 服务器
- ✅ YAML 声明式流水线，版本化管理
- ✅ 多环境部署 + 灰度发布支持
- ⚠️ 两套语法差异，迁移需重写流水线
- ⚠️ GitHub Actions 自托管 Runner 运维成本

**替代方案**：
- Jenkins：功能最强但配置冗长、UI 差用、维护成本高
- Tekton：K8s 原生但学习曲线陡
- ArgoCD：GitOps 部署优秀但不做 CI 构建

**参与人**：
- 提案人：DevOps 负责人
- 评审人：架构师
- 决策日期：2026-07-27

---

#### ADR-024：镜像仓库选用 Harbor

**状态**：已接受

**背景**：
- K8s 集群需私有镜像仓库
- 国内网络访问 Docker Hub 不稳定
- 需镜像漏洞扫描与 RBAC 权限控制

**决策**：
Harbor 作为私有镜像仓库，配合 Trivy 漏洞扫描。

**后果**：
- ✅ 国内可达，镜像推拉稳定
- ✅ RBAC 项目级权限控制
- ✅ 内置漏洞扫描（Trivy/Clair）
- ⚠️ Harbor 自身需高可用部署（PostgreSQL + Redis）
- ⚠️ 镜像清理策略需配置，避免存储膨胀

**替代方案**：
- Docker Registry：无权限控制、无漏洞扫描
- 云厂商 ACR/ECR：商业产品，锁定云厂商
- Nexus：功能臃肿，镜像管理弱于 Harbor

**参与人**：
- 提案人：DevOps 负责人
- 评审人：运维负责人
- 决策日期：2026-07-27

---

#### ADR-025：第三方支付接入支付宝开放平台

**状态**：已接受

**背景**：
- 需求明确指定支付宝作为唯一支付渠道
- 支付宝开放平台提供支付/退款/提现/对账完整能力
- 异步回调需验签 + 幂等消费

**决策**：
接入支付宝开放平台，支付/退款/提现走支付宝 API，异步回调验签 + 幂等键（trade_no + 商户单号）。

**后果**：
- ✅ 支付宝市场份额最大，用户覆盖广
- ✅ 文档完善，沙箱环境可本地调试
- ⚠️ 单一支付渠道，支付宝故障则支付不可用（需降级文档）
- ⚠️ 回调接口必须幂等，重复通知不可重复入账

**替代方案**：
- 微信支付：需求未指定，后续可扩展
- 聚合支付（收钱吧/支付宝服务商）：多一层依赖，首期不引入
- 银联在线：用户体验差，首期不引入

**参与人**：
- 提案人：产品负责人
- 评审人：架构师、支付研发
- 决策日期：2026-07-27

---

#### ADR-026：排除 Dubbo + Triple

**状态**：已接受

**背景**：
- 项目已选择 OpenFeign REST 风格作为服务间通信协议（见 ADR-007）
- Dubbo + Triple 是 RPC 风格，与 REST 风格不统一
- 团队无 Dubbo 经验

**决策**：
排除 Dubbo + Triple，保持 OpenFeign REST 风格统一。

**后果**：
- ✅ 全链路 REST 风格一致，端到端调试链路统一
- ✅ HTTP 协议通用，网关/浏览器/Postman 均可直接调用
- ⚠️ REST 性能弱于 RPC（序列化开销 + HTTP 协议开销）
- ⚠️ 后续若需超高性能内部调用需重新评估

**替代方案**：
- 保留 Dubbo + Triple：风格分裂，维护成本高
- Dubbo + REST 双协议：复杂度翻倍，首期不引入

**参与人**：
- 提案人：架构师
- 评审人：3 名核心研发
- 决策日期：2026-07-27

---

#### ADR-027：排除 Spring Cloud Sleuth

**状态**：已接受

**背景**：
- 项目已选择 SkyWalking 9.x 作为链路追踪方案（见 ADR-019）
- Spring Cloud Sleuth 已停止维护，官方推荐迁移到 Micrometer Tracing + OpenTelemetry

**决策**：
排除 Sleuth，使用 SkyWalking Java Agent 无侵入探针，不引入 Micrometer Tracing 避免双套追踪体系。

**后果**：
- ✅ 无侵入，零业务代码改动
- ✅ 不引入额外依赖和配置
- ⚠️ Agent 升级需重启服务
- ⚠️ 如需 SDK 层面手动创建 Span，需引入 OpenTelemetry SDK

**替代方案**：
- Micrometer Tracing + OpenTelemetry：侵入式，需改代码，首期不引入
- Sleuth + Zipkin：已停止维护，安全风险

**参与人**：
- 提案人：架构师
- 评审人：2 名后端研发
- 决策日期：2026-07-27

---

#### ADR-028：排除 Eureka

**状态**：已接受

**背景**：
- Spring Cloud Netflix 已停止积极维护
- 项目已选择 Nacos 2.3+ 作为注册中心（见 ADR-003），Nacos 功能全面替代 Eureka

**决策**：
排除 Eureka，使用 Nacos 作为注册中心。

**后果**：
- ✅ 避免依赖已停维组件，安全漏洞无修复风险
- ✅ Nacos 功能远超 Eureka（配置中心 + 动态路由 + 健康检查）
- ⚠️ 从 Eureka 迁移需改依赖和配置（团队已有 Nacos 经验，成本可控）

**替代方案**：
- 保留 Eureka：停维风险、功能受限
- Consul：可替代但团队无经验

**参与人**：
- 提案人：架构师
- 评审人：3 名核心研发
- 决策日期：2026-07-27

---

#### ADR-029：排除 OpenFeign 作为异步/高并发调用方式

**状态**：已接受

**背景**：
- 当前 OpenFeign 基于 Spring Web MVC 同步阻塞模型（见 ADR-007 / ADR-013）
- 高并发场景下线程池耗尽可能成为瓶颈
- ⚠️ 虚拟线程需 JDK 21，本期 JDK 17 不启用

**决策**：
排除 OpenFeign 作为异步/高并发调用方式。OpenFeign 保留用于同步低并发内部调用（如查库存、查用户），异步/高并发场景使用 RocketMQ 消息或 CompletableFuture+线程池优化 IO。后续根据压测数据评估迁移到 WebClient（响应式）。

切换阈值：
- 并发 > 100 QPS 或 单次 RT > 200ms 的内部调用 → 使用 RocketMQ 消息或 CompletableFuture+线程池
- 低于阈值 → 使用 OpenFeign 同步调用
- 阈值可通过 Nacos 配置动态调整：`minimall.feign.concurrency-threshold`、`minimall.feign.rt-threshold-ms`

**后果**：
- ✅ 首期开发效率最高，复杂度最低
- ✅ 明确记录技术债，有后续演进路径
- ✅ 异步/高并发场景有明确方案（见 ADR-029.1）
- ⚠️ 高并发场景需调大线程池 + 监控线程池耗尽告警
- ⚠️ 若评估后需迁移 WebClient，全链路响应式改造成本高
- ⚠️ 虚拟线程需 JDK 21，本期 JDK 17 不启用，不纳入评估

**替代方案**：
- 立即迁移 WebClient：首期过重，团队不熟悉
- 启用虚拟线程：需 JDK 21，本期不适用
- 保持现状不记录：技术债无追踪，后续翻盘无法追溯

**参与人**：
- 提案人：架构师
- 评审人：3 名核心研发
- 决策日期：2026-07-27

---

#### ADR-029.1：异步调用方案

**状态**：已接受

**背景**：
- ADR-029 已排除 OpenFeign 作为异步/高并发调用方式
- 高并发场景（如批量通知、异步对账、ES 索引同步）需非阻塞异步调用
- 本期 JDK 17，虚拟线程不可用，需基于线程池的异步方案

**决策**：
异步/高并发场景采用两种方案：
1. **RocketMQ 消息**：跨服务异步事件广播（如订单状态变更通知、ES 索引同步），保证最终一致
2. **CompletableFuture + 线程池**：服务内异步编排（如并行查询多个下游），线程池参数按场景隔离配置

**后果**：
- ✅ 无需引入响应式框架（WebFlux），降低学习成本
- ✅ MQ 方案天然支持重试 + 死信，可靠性高
- ⚠️ CompletableFuture 需注意线程池隔离，不同业务不可共用线程池避免相互影响
- ⚠️ 异步调用链路追踪需手动透传 MDC 上下文（traceId 等）

**替代方案**：
- WebClient 响应式：全链路改造成本高，首期不引入
- 虚拟线程：需 JDK 21，本期不适用
- 纯同步调用：高并发场景线程池耗尽风险

**参与人**：
- 提案人：架构师
- 评审人：3 名核心研发
- 决策日期：2026-07-27

---

#### ADR-030：选用 XXL-JOB 3.0.0+ 作为分布式定时任务调度中心

**状态**：已接受

**背景**：
- 订单超时取消、支付超时关单、库存同步定时任务等场景需要可靠分布式调度
- Spring `@Scheduled` 无法满足集群去重和失败重试

**决策**：
选用 XXL-JOB 3.0.0+，轻量级、与 Spring Boot 集成简单、支持 GLUE 模式、失败告警、路由策略丰富。

**后果**：
- ✅ 轻量级，部署简单，与 Spring Boot 集成仅需引入 `xxl-job-core` 依赖
- ✅ 社区活跃，生产验证充分
- ✅ 失败告警机制完善（邮件/钉钉/企微）
- ✅ 路由策略丰富（轮询、随机、一致性哈希、故障转移等）
- ❌ 需额外部署 `xxl-job-admin` 管理控制台
- ⚠️ XXL-JOB 2.4.x 使用 `javax.annotation.Resource`，与 Spring Boot 3.x (Jakarta EE 9+) 不兼容。必须使用 3.0.0+ 版本，该版本已迁移至 `jakarta` 命名空间。

**替代方案**：
- Spring `@Scheduled`：单机调度，集群重复执行，无失败重试
- ElasticJob：功能强但依赖 ZooKeeper，运维成本高
- Quartz 集群：数据库锁竞争，性能瓶颈

**参与人**：
- 提案人：架构师
- 评审人：2 名后端研发
- 决策日期：2026-07-27

---

#### ADR-031：容器化非功能需求

**状态**：已接受

**背景**：
- 微服务容器化部署需统一基础镜像、资源限制、优雅下线策略
- 缺少统一规范导致各服务镜像大小、资源配额、下线行为不一致

**决策**：
1. JDK 基础镜像：`eclipse-temurin:17-jre-alpine`（精简，~170MB）
2. 资源限制：requests(cpu=200m, memory=256Mi), limits(cpu=1, memory=1Gi)
3. 优雅下线：SIGTERM → preStopHook(sleep 15) → Spring Boot graceful shutdown(30s) → K8s 终止
4. 健康检查：`/actuator/health`（liveness）+ `/actuator/health/readiness`（readiness）

**后果**：
- ✅ 统一镜像减少运维成本，alpine 镜像体积小、启动快
- ✅ 优雅下线避免请求丢失，preStopHook 留出 K8s Service endpoints 更新时间
- ❌ alpine 镜像缺少 glibc 调试工具，排障时可能需临时切换镜像

**替代方案**：
- `eclipse-temurin:17-jre`（基于 Debian）：体积大（~450MB），含完整调试工具
- `openjdk:17-slim`：社区维护，更新频率低于 Temurin

**参与人**：
- 提案人：架构师
- 评审人：DevOps 负责人、SRE
- 决策日期：2026-07-28

---

#### ADR-032：选用 GitHub Actions 作为 CI/CD 平台

**状态**：已接受

**背景**：
- 项目需要自动化构建、测试、部署流水线
- 代码托管在 GitHub，CI/CD 应与代码平台深度集成
- ADR-023 已记录 CI/CD 选型为 GitLab CI / GitHub Actions 任选，现需确定唯一方案

**决策**：
选用 GitHub Actions，使用 `.github/workflows/` 目录定义流水线。

**后果**：
- ✅ 与 GitHub 生态深度集成，PR 状态检查、分支保护零配置
- ✅ 自托管 Runner 支持K8s部署，镜像构建推送直接在集群内完成
- ✅ YAML 声明式流水线，版本化管理，可回溯
- ❌ 自建 Runner 需运维（监控、扩容、升级）

**替代方案**：
- GitLab CI：需 GitLab Runner，与 GitHub 托管不匹配
- Jenkins：功能最强但配置冗长、UI 差用、维护成本高
- Tekton：K8s 原生但学习曲线陡

**参与人**：
- 提案人：架构师
- 评审人：DevOps 负责人
- 决策日期：2026-07-28

---

#### ADR-033：选用 HashiCorp Vault 作为 KMS

**状态**：已接受

**背景**：
- 数据库密码、支付密钥、JWT 签名密钥等敏感配置需加密托管
- Nacos 配置中心存加密值，需 KMS 管理解密密钥
- 禁止 `application.yml` 明文存储敏感信息

**决策**：
选用 HashiCorp Vault，Nacos 配置中心存加密值，Vault 管理解密密钥。应用启动时通过 Vault AppRole 认证获取密钥，解密 Nacos 中的加密配置。

**后果**：
- ✅ 开源 + 功能全 + 动态密钥轮换
- ✅ 支持密钥版本管理、审计日志、细粒度策略
- ✅ AppRole 认证模式适配 K8s ServiceAccount
- ❌ 需额外部署 Vault 集群 + 运维成本
- ❌ 开发环境需简化部署（dev 模式可用 Vault dev server）

**替代方案**：
- Nacos 内置 AES 加密：仅解决配置传输加密，密钥管理仍需外部 KMS
- 云厂商 KMS（阿里云 KMS）：供应商锁定，本地开发环境不可用
- Spring Cloud Vault：Vault 客户端库，需 Vault Server 配合

**参与人**：
- 提案人：架构师
- 评审人：运维负责人、安全负责人
- 决策日期：2026-07-28

---

#### ADR-034：选用 SSE 实现实时推送

**状态**：已接受

**背景**：
- 订单状态变更、库存预警需实时推送到客户端
- 当前需求为服务端 → 客户端单向推送，无需双向通信
- WebSocket 双向通信增加复杂度且当前场景不需要客户端主动推送

**决策**：
使用 Server-Sent Events (SSE) 单向推送，不做 WebSocket 双向通信。

**后果**：
- ✅ HTTP 原生协议，无需额外握手，浏览器原生 `EventSource` API
- ✅ 自动重连 + 事件ID断点续传
- ✅ 实现简单，网关/CDN 友好
- ❌ 仅服务 → 客户端单向，客户端不可通过同连接向服务端发消息
- ❌ IE 不支持（项目已排除 IE）

**替代方案**：
- WebSocket：双向通信能力强，但当前场景不需要，增加复杂度
- 长轮询：兼容性最好但资源浪费大
- MQTT：过重，需额外 Broker

**参与人**：
- 提案人：架构师
- 评审人：2 名后端研发、前端负责人
- 决策日期：2026-07-28

---

#### ADR-035：选用 阿里云SMS+邮件推送

**状态**：已接受

**背景**：
- notify-service 需短信和邮件发送能力
- 团队已有阿里云使用经验
- 国内短信到达率是核心指标

**决策**：
阿里云短信服务（SMS）+ 阿里云邮件推送（DirectMail）。

**后果**：
- ✅ 同生态，统一账号/计费/工单
- ✅ API 简单，SDK 成熟，Spring Boot 集成成本低
- ✅ 国内到达率高，合规备案流程完善
- ❌ 供应商锁定，切换需改 SDK + 重新备案
- ❌ 海外用户到达率需评估

**替代方案**：
- 腾讯云 SMS+SES：供应商不同但能力相当，团队无经验
- Twilio/SendGrid：海外为主，国内到达率低
- 自建邮件服务器：运维成本高，IP 信誉难维护

**参与人**：
- 提案人：架构师
- 评审人：2 名后端研发
- 决策日期：2026-07-28

---

#### ADR-036：灰度发布方案 — Gateway+Nacos元数据路由

**状态**：已接受

**背景**：
- 生产发布需灰度能力，降低全量发布风险
- 已有 Spring Cloud Gateway + Nacos 基础设施，无需引入额外组件
- 需支持按请求头/Cookie 染色路由到灰度实例

**决策**：
Spring Cloud Gateway 按请求头/Cookie 染色路由到灰度实例，Nacos 服务实例 metadata 标记版本（如 `version=gray`）。Gateway 自定义 GlobalFilter 读取染色标记，匹配 metadata 路由。

**后果**：
- ✅ 轻量，无需额外组件（Istio/Argo Rollouts）
- ✅ 与现有 Gateway + Nacos 基础设施零额外成本
- ✅ 染色规则可通过 Nacos 配置热更新
- ❌ 仅限 Spring Cloud 生态，跨语言服务不适用
- ❌ 灰度比例控制粒度粗（实例级，非流量百分比级）

**替代方案**：
- Istio：功能最强但引入整个 Service Mesh，首期过重
- Argo Rollouts：K8s 原生灰度，但需 Gateway 配合，且学习成本
- Nginx Ingress Canary：需额外 Ingress 配置，与 Gateway 架构不一致

**参与人**：
- 提案人：架构师
- 评审人：DevOps 负责人、2 名后端研发
- 决策日期：2026-07-28

---

#### ADR-037：首期不做数据库读写分离

**状态**：已接受

**背景**：
- MVP 阶段 QPS 预估 < 2000，单主库足够
- 读写分离增加运维复杂度（主从延迟监控、路由规则、一致性选择）
- 数据正确性 > 一切（架构原则 P0），主从延迟可能导致读到旧数据

**决策**：
首期单主库，MyBatis-Plus `dynamic-datasource` 配置预留从库数据源位，压测后按需加从库。

**后果**：
- ✅ 降低首期复杂度，聚焦核心业务
- ✅ 预留配置位，后续开启读写分离仅需改配置 + 加从库
- ⚠️ 高 QPS 场景需手动加从库 + 开启读写分离
- ⚠️ 预留配置需在首期代码中验证，避免后续不兼容

**替代方案**：
- 首期即做读写分离：增加运维成本，MVP 阶段 ROI 低
- ShardingSphere 读写分离：功能强但首期过重

**参与人**：
- 提案人：架构师
- 评审人：DBA、2 名后端研发
- 决策日期：2026-07-28

---

#### ADR-038：数据库版本管理选用 Flyway

**状态**：已接受

**背景**：
- 10 个微服务 + 10 个独立数据库，DDL 变更管理混乱必出事故
- 需要版本化、可追溯、自动化执行的数据库迁移方案
- Spring Boot 原生集成简化运维

**决策**：
选用 Flyway 作为数据库版本管理工具，每个服务在 `resources/db/migration/` 目录下管理自己的 DDL 脚本。脚本命名规则：`V{version}__{description}.sql`（如 `V1.0.0__init_schema.sql`）。

**后果**：
- ✅ SQL-first，简单可控，Spring Boot 启动自动执行
- ✅ 版本化管理，变更可追溯
- ✅ 每服务自治，避免集中管理单点故障
- ⚠️ 脚本执行后不可修改，需新建版本脚本
- ⚠️ 生产环境需禁用自动执行，由 CI/CD 流水线显式调用

**替代方案**：
- Liquibase：YAML/XML/SQL 多格式，回滚支持好但配置重
- 手动 SQL 脚本：DBA 统一管理，最安全但最慢

**参与人**：
- 提案人：架构师
- 评审人：DBA、2 名后端研发
- 决策日期：2026-07-28

---

#### ADR-039：认证机制采用 Access+Refresh 双 token

**状态**：已接受

**背景**：
- JWT 方案已定（ADR-014 网关安全增强），但缺少实现细节
- 单 Access token 方案无法主动吊销，安全隐患大
- 需要兼顾安全性与用户体验

**决策**：
采用 Access token (15min) + Refresh token (7 天) 双 token 机制。Refresh token 存储于 Redis（可主动吊销），Access token 无状态不存。

**后果**：
- ✅ Access token 短期有效，泄露风险可控
- ✅ Refresh token 可主动吊销（Redis 删除即可）
- ✅ 兼顾安全与体验，用户 7 天内免登录
- ⚠️ Redis 需高可用，否则无法刷新 token
- ⚠️ 需要 token 刷新接口 `/api/v1/auth/refresh-token`

**替代方案**：
- 单 Access token：无法主动吊销，安全风险高
- Access+Refresh+Online 白名单：最复杂，可踢人但成本高

**参与人**：
- 提案人：架构师
- 评审人：安全负责人、2 名后端研发
- 决策日期：2026-07-28

---

#### ADR-047：分布式锁选用 Redisson 可重入锁

**状态**：已接受

**背景**：
- 库存扣减、订单创建等场景需防并发
- 需要可重入锁支持同一线程多次获取
- 需要自动续期避免业务未完成锁过期

**决策**：
选用 Redisson 可重入锁，看门狗自动续期，Lua 脚本保证原子性，与 Spring Boot 集成简单。

**后果**：
- ✅ 可重入锁，同一线程多次获取不死锁
- ✅ 看门狗自动续期，业务未完成不会过期
- ✅ Lua 脚本保证加锁/解锁原子性
- ⚠️ 需要正确配置 watchdog 超时时间（默认 30s）
- ⚠️ 业务需合理设置锁粒度，避免长持锁

**替代方案**：
- Redis SETNX + 过期：无看门狗，业务未完成可能过期
- 数据库悲观锁：SELECT FOR UPDATE，性能差，高并发易死锁

**参与人**：
- 提案人：架构师
- 评审人：2 名后端研发
- 决策日期：2026-07-28

---

#### ADR-051：业务异常码按服务分段

**状态**：已接受

**背景**：
- 项目已定 Result<T> 返回，成功码 00000，业务错误 1xxxx
- 各服务异常码无规划易冲突
- 需要统一分类便于排查与监控

**决策**：
业务异常码按服务分段：
- 用户域：10000-10999
- 商品域：11000-11999
- 订单域：12000-12999
- 支付域：13000-13999

各服务枚举定义，互不冲突。

**后果**：
- ✅ 服务间异常码不冲突，监控告警可按服务聚合
- ✅ 排查问题可快速定位服务
- ⚠️ 每个服务需预留足够空间（1000 个够用）
- ⚠️ 新增服务需分配新段

**替代方案**：
- 按错误类型分段：服务间协调复杂
- 不分段自然增长：易冲突，无规划

**参与人**：
- 提案人：架构师
- 评审人：2 名后端研发
- 决策日期：2026-07-28

---

#### ADR-052：消息可靠性采用 RocketMQ 事务消息

**状态**：已接受

**背景**：
- 订单创建 → 库存扣减 → 支付，需保证消息不丢不重
- 本地事务与消息发送需原子性
- RocketMQ 原生支持事务消息

**决策**：
采用 RocketMQ 事务消息，半消息机制保证本地事务与消息发送原子性。

**后果**：
- ✅ 半消息机制，本地事务与消息发送原子性保证
- ✅ RocketMQ 原生支持，无需本地消息表
- ⚠️ 需实现 LocalTransactionChecker 回查接口
- ⚠️ 事务消息吞吐量略低于普通消息

**替代方案**：
- 本地消息表：可靠但需额外表 + 定时任务
- 普通消息 + 回调：极端情况可能丢消息

**参与人**：
- 提案人：架构师
- 评审人：2 名后端研发
- 决策日期：2026-07-28

---

#### ADR-055：缓存一致性采用 Cache-Aside + 延迟双删

**状态**：已接受

**背景**：
- DB 更新后需同步 Redis 缓存
- 先删缓存再更新 DB 可能读旧数据回填
- 需要兼顾一致性与性能

**决策**：
采用 Cache-Aside 模式，先更新 DB 再删除缓存，延迟双删防脏数据。

**后果**：
- ✅ 先更新 DB 保证数据正确性
- ✅ 延迟双删防止并发读旧数据回填缓存
- ⚠️ 延迟时间需大于主从同步延迟 + 查询耗时
- ⚠️ 极端情况仍可能有短暂不一致

**替代方案**：
- 先删缓存再更新 DB：并发时脏数据风险高
- 订阅 Binlog 同步：解耦但架构复杂，需 Canal

**参与人**：
- 提案人：架构师
- 评审人：2 名后端研发
- 决策日期：2026-07-28

---

#### ADR-056：敏感数据采用 AES-256 字段级加密

**状态**：已接受

**背景**：
- 用户手机号、身份证、支付密码等敏感字段需加密存储
- 密钥需安全托管，禁止硬编码
- 需支持模糊查询（手机号、身份证）

**决策**：
采用 AES-256 字段级加密，密钥托管于 Vault。手机号/身份证保留前 3 后 4 支持模糊查询，密码/银行卡号单向哈希（BCrypt/SHA-256）。

**后果**：
- ✅ 敏感数据加密存储，符合安全合规要求
- ✅ Vault 托管密钥，支持版本管理与审计
- ✅ 手机号/身份证支持模糊查询
- ⚠️ 加密字段查询性能下降，需合理设计索引
- ⚠️ Vault 需高可用部署

**替代方案**：
- 透明加密：MySQL 企业版 TDE，需商业授权
- 不加密仅脱敏：安全风险高，不符合合规要求

**参与人**：
- 提案人：架构师
- 评审人：安全负责人、2 名后端研发
- 决策日期：2026-07-28

---

#### ADR-057：慢 SQL 监控阈值为 1 秒

**状态**：已接受

**背景**：
- Druid 已配置 StatFilter（ADR-002.1），需确定慢 SQL 阈值
- 阈值过严误报多，阈值过宽错过性能问题
- 生产环境需合理阈值便于排查优化

**决策**：
慢 SQL 阈值设为 1 秒，超过 1 秒记录慢 SQL 日志。

**后果**：
- ✅ 1 秒为生产环境合理阈值，便于排查优化
- ⚠️ 高频简单查询 1 秒已属异常，需优化
- ⚠️ 复杂报表查询可能超 1 秒，需单独评估

**替代方案**：
- 500 毫秒：严格阈值，正常查询可能误报
- 3 秒：宽松阈值，可能错过性能问题

**参与人**：
- 提案人：架构师
- 评审人：DBA、2 名后端研发
- 决策日期：2026-07-28

---

#### ADR-061：Druid 连接池参数

**状态**：已接受

**背景**：
- Druid 已选型（ADR-002.1），需确定连接池参数
- 参数影响数据库连接资源与性能
- 中小规模服务需合理配置避免资源浪费

**决策**：
Druid 连接池参数配置：
- 初始连接：5 个
- 最小空闲：5 个
- 最大连接：20 个
- 获取连接超时：60 秒
- 空闲检测：300 秒

**后果**：
- ✅ 中小规模服务够用，避免连接泄漏
- ✅ 资源占用可控，不会过多占用数据库连接
- ⚠️ 高并发服务需按压测数据调整最大连接数
- ⚠️ 需监控连接池使用率，及时告警

**替代方案**：
- 初始 10 + 最大 50：高并发场景，但连接数过多占用资源
- 默认配置：风险不可控

**参与人**：
- 提案人：架构师
- 评审人：DBA、2 名后端研发
- 决策日期：2026-07-28

---

#### ADR-062：Feign 超时配置

**状态**：已接受

**背景**：
- Feign 调用远程服务需合理超时配置
- 默认超时配置不合理，可能无限等待
- 需要平衡快速失败与业务耗时

**决策**：
Feign 超时配置：
- 连接超时：5 秒（建立 TCP 连接）
- 读取超时：10 秒（等待响应）
- 重试：1 次（网络抖动场景）

**后果**：
- ✅ 简单查询够用，复杂业务可单独配置
- ✅ 重试 1 次应对网络抖动，避免过度重试
- ⚠️ 复杂业务（如支付回调）需单独配置更长超时
- ⚠️ 需监控 Feign 调用耗时，及时调整

**替代方案**：
- 连接 3 秒 + 读取 5 秒：快速失败，但复杂业务可能超时
- 无超时：风险不可控，可能无限等待

**参与人**：
- 提案人：架构师
- 评审人：2 名后端研发
- 决策日期：2026-07-28

### 4. ADR 维护规则

- 一旦发布，**不得修改**，仅可标记"已取代"并链接新 ADR
- 每次重要技术决策**必须**新增 ADR
- ADR 评审在 [团队协作流程](../6.%20项目管理/04-团队协作流程.md) 的"技术决策会"中完成
- 序号严格递增，禁止重用

### 5. 反模式

- ❌ 仅口头/会议记录，无书面 ADR
- ❌ 修改历史 ADR 掩盖错误决策
- ❌ 用 ADR 记录 Bug 修复
- ❌ 一次决策不写 ADR，决策翻盘时无法追溯

## 相关文档链接

- 上游：[系统架构设计文档](01-系统架构设计文档.md)
- 横向：[模块概要设计](06-模块概要设计.md)
- 下游：所有开发、测试、部署文档均需引用相关 ADR

## 变更记录

| 日期 | 版本 | 变更人/角色 | 变更说明 |
|------|------|-------------|----------|
| 2026-07-27 | V1.0 | yirancrazy@gmail.com | 初稿创建，登记 ADR-001 ~ ADR-010 |
| 2026-07-27 | V2.0 | yirancrazy@gmail.com | 扩充至 ADR-001 ~ ADR-029：补全架构§5全维度选型（ADR-011~025）；新增排除项 ADR（ADR-026~029）；替换所有 ${VERSION} 占位符为真实版本号；每条 ADR 补全替代方案与参与人 |
| 2026-07-28 | V2.1 | yirancrazy@gmail.com | 新增 ADR-030~037：XXL-JOB、容器化、CI/CD、KMS、SSE、SMS、灰度发布、读写分离 |
| 2026-07-28 | V3.0 | yirancrazy@gmail.com | 新增 ADR-038~062：Flyway、双token认证、JSON日志、API版本、Maven三层、Docker Compose、手写代码、枚举存储、Feign异常、Redisson锁、Caffeine缓存、幂等Token、SpringDoc+Knife4j、异常码分段、RocketMQ事务/顺序消息、分库分表预留、Cache-Aside、AES-256加密、慢SQL阈值、日志保留、邮件告警、Actuator健康检查、Druid连接池参数、Feign超时配置 |
