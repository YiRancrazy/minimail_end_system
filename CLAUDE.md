# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> 下方正文为 **V1.0 项目级硬约束**（命名 / 分层 / 日志 / 异常 / 实体 / 测试），速查够用时无需展开任何 `.dev/docs/` 链接。Commands 与 Architecture 总览见下方两个速查小节，其他细节按需加载 §7「加载触发矩阵」对应文档。

## Quick reference

- **Stack**: Java 17 · Spring Boot 3.2.x · Spring Cloud Alibaba · MyBatis-Plus 3.5.x · Maven 3.8.6 · Nacos · Seata · RocketMQ · SkyWalking
- **Layout**: Maven 多模块，父聚合 `pom.xml` + `minimall-common` + `minimall-api` + `minimall-gateway` + 10 业务服务子模块（auth / user / merchant / goods / cart / order / pay / stock / notify / platform）。详见 [.dev/docs/3. 开发阶段/04-项目目录结构规范.md](.dev/docs/3.%20开发阶段/04-项目目录结构规范.md)。
- **依赖方向**（强制）：业务服务 → `minimall-api` → `minimall-common`；`common` / `api` **禁止**反向依赖任何服务模块；服务间**只能**走 Feign。

## Commands

通过 Maven Wrapper 统一执行（`./mvnw`，Windows 上用 `mvnw.cmd`）。本地中间件（MySQL / Redis / Nacos）通过 Docker Compose 启动，详见 [.dev/docs/3. 开发阶段/05-开发环境搭建指南.md](.dev/docs/3.%20开发阶段/05-开发环境搭建指南.md) §7。

| 用途 | 命令 |
|------|------|
| 全量构建（跳过测试） | `./mvnw clean package -DskipTests` |
| 启动单个服务（默认 dev profile） | `./mvnw -pl minimall-<service>-service spring-boot:run` |
| 切换 profile 启动 | `./mvnw spring-boot:run -Dspring-boot.run.profiles=prod` |
| 跑全量单测（自动切 `test` profile + H2） | `./mvnw test` |
| 跑单个服务的单测 | `./mvnw -pl minimall-<service>-service test` |
| 跑单个测试类 | `./mvnw -pl minimall-<service>-service test -Dtest=ClassName` |
| 跑单个测试方法 | `./mvnw -pl minimall-<service>-service test -Dtest=ClassName#methodName` |
| 静态扫描（CI 卡点） | `./mvnw checkstyle:check && ./mvnw com.github.spotbugs:spotbugs-maven-plugin:check && ./mvnw org.owasp:dependency-check-maven:check` |

Profile → 中间件差异：dev 用本地 Docker Compose；prod 走 Nacos + KMS；test 自动切 H2 + Mock（无需 Docker）。CI 流水线 4 段（build → test → scan → docker）见 [.dev/docs/5. 部署阶段/01-构建与部署文档（CI-CD）.md](.dev/docs/5.%20部署阶段/01-构建与部署文档%28CI-CD%29.md)。

## Architecture snapshot

系统为 **Spring Cloud Alibaba 微服务**，外部流量统一经 `minimall-gateway:8080` 进入；每服务独占逻辑库，跨服务**禁止直连 DB**，仅允许 Feign / RocketMQ。架构图、C4-Container、链路时序、数据一致性边界、可观测性三件套见 [.dev/docs/2. 设计阶段/01-系统架构设计文档.md](.dev/docs/2.%20设计阶段/01-系统架构设计文档.md)。本节只给速查骨架。

**服务清单**（端口 → 库 → 职责）：

| 端口 | 服务 | 库 | 职责 |
|------|------|------|------|
| 8080 | `minimall-gateway` | — | 路由、JWT 验签、Sentinel 限流、灰度 |
| 8201 | `minimall-auth-service` | `auth_db` | 登录/注册/找回/token/RBAC |
| 8202 | `minimall-user-service` | `user_db` | C 端档案/地址/收藏 |
| 8203 | `minimall-merchant-service` | `merchant_db` | B 端档案/店铺资质/审核 |
| 8204 | `minimall-goods-service` | `goods_db` | SPU/SKU/上下架/ES 同步 |
| 8205 | `minimall-cart-service` | `cart_db` | 购物车 CRUD |
| 8206 | `minimall-order-service` | `order_db` | 订单生命周期/物流/售后 |
| 8207 | `minimall-pay-service` | `pay_db` | 支付宝/退款/对账/流水 |
| 8208 | `minimall-stock-service` | `stock_db` | 库存初始化/调整/调拨/盘点 |
| 8209 | `minimall-notify-service` | `notify_db` | 站内信/WS/短信/邮件 |
| 8210 | `minimall-platform-service` | `platform_db` | 平台管理员/对账/公告/强制关单 |

**分层契约**（强绑定 MyBatis-Plus 规范，详见 [.dev/docs/3. 开发阶段/03-MyBatis-Plus规范.md](.dev/docs/3.%20开发阶段/03-MyBatis-Plus规范.md)）：

- `Controller`：放 `controller/v1/`，类名必须带 `V1` 后缀（如 `OrderControllerV1`），返 `Result<XxxVO>`，**禁止** `try-catch` 吞异常
- `Service`：薄业务层；`XxxServiceImpl` **禁止**继承 `IService<T>` / `ServiceImpl<M, T>`
- `Manager`：跨 Mapper / 跨聚合编排；`XxxManagerImpl` **必须**继承 `IService<T>` + `ServiceImpl<M, T>`
- POJO 边界：`XxxPO`（库）/ `XxxDTO`（入参）/ `XxxVO`（出参）/ `XxxBO`（Service 内）
- 跨服务传输用 `minimall-api` 里的 `XxxDTO`，**禁止**直接暴露 PO

**核心链路**：下单 = Gateway → `order-service` 启 Seata AT 全局事务 → 调 `stock-service` 锁库存 → `pay-service` 创建支付流水 → 支付宝异步回调 → 事务消息 `ORDER_PAID` 广播 → `order-service` 推进状态 / `notify-service` 发站内信。链路时序图见架构文档 §6.1。

**中间件**：MySQL 8（逻辑库隔离）/ Redis 7.2.4（Cluster）/ RocketMQ 5.x（事务消息）/ Elasticsearch 8（商品搜索）/ Nacos 2.3（注册+配置）/ Seata 2.x（AT 分布式事务）/ Sentinel（限流熔断）/ SkyWalking 9（Trace）/ Prometheus + Grafana + Loki（Metrics + Logs）。

---

# Java 后端 · 项目级 AI 协作约定

## 文档信息

| 字段 | 内容 |
|------|------|
| 文档名称 | Java 后端 · 项目级 AI 协作约定 |
| 文档版本 | V1.0 |
| 文档状态 | 已发布 |
| 所属技术栈 | Java |
| 创建人 | yirancrazy@gmail.com |
| 创建日期 | 2026-07-27 |
| 最后更新 | 2026-07-27 |

## 文档目的

> 沉淀 Java 后端**项目级硬约束**（构建 / 命名 / 分层 / 日志 / 异常 / 实体 / 测试），供 AI 与新成员速查。
> 跨栈元规则见仓库根 `CLAUDE.md`；具体规范按需加载「加载触发矩阵」。

## 适用范围

- **适用对象**：Java 后端研发、测试、DevOps
- **适用场景**：日常编码、Code Review、CI 卡点、新成员 onboarding
- **不适用范围**：具体实现细节 → 见「相关文档链接」按需加载

## 详细内容

### 1. AI 协作约定

- **功能设计 / 创意发散** → 调用 `brainstorming` skill
- **设计落地 / 跨文件决策 / 本文件改动** → 调用 `grilling` skill
- 改代码前先用 `Glob`/`Grep` 定位相关文件，**只读必要的几个**，不要一次性铺开读
- 跨模块改动 / 根因不明的 bug：先看目录结构 + 调一遍调用链，再动手；别只盯单个文件
- 方案设计类任务（新功能、重构）：先理清模块边界再写

### 2. 项目元数据

| 字段 | 内容 |
|------|------|
| 项目名称 | 薄荷商城后台系统 |
| 包名前缀 | `com.yirancrazy.minimall` |
| JDK | 17（LTS） |
| 启动 Profile | `dev`（默认）/ `test` / `staging` / `prod` |

### 2.1 开发迭代

| 迭代 | 主题 | 范围 |
|------|------|------|
| Iter-0 | 基建 | common + api + gateway + id-service + Seata/RMQ/Nacos 基础设施 |
| Iter-1 | 核心CRUD | auth + user + merchant + goods + cart |
| Iter-2 | 交易链路 | stock → pay → order → notify → platform |

### 3. 构建 & 运行

- **运行**：`./mvnw spring-boot:run`（默认 profile = `dev`）
- **打包**：`./mvnw clean package -DskipTests`
- **单测**：`./mvnw test`（profile = `test`，自动切 H2）
- **切换 profile**：`./mvnw spring-boot:run -Dspring-boot.run.profiles=prod`

### 4. 命名 & 约定

只列项目特有约束；通用命名/POJO 风格见 [01-Java编码规范](.dev/docs/3.%20开发阶段/01-Java编码规范.md)。

- **Controller 后缀**：全部带 `V1` 版本后缀（如 `UserControllerV1`）
- **统一返回**：`Result<T>`，code 遵循《阿里规约 黄山版》：成功 `"00000"`、业务错误 `1xxxx`、系统错误 `2xxxx`；构造用 `Result.success(data)` / `Result.fail(code, msg)`
- **分层契约**（与 MyBatis-Plus 规范强绑定）：
  - **Service**：薄业务层，**禁止**继承 `IService<T>` / `ServiceImpl<M, T>`
  - **Manager**：跨 Mapper / 跨聚合编排层，**必须**继承 `IService<T>` + `ServiceImpl<M, T>` 复用 MP 通用 CRUD
  - **枚举**：放 `constant/`，字段命名 `(code, alias, message)`

### 5. 关键约定

#### 5.1 日志

- 统一 SLF4J + Logback，业务类上 `@Slf4j`
- **占位符**：`log.info("user={}", userId)`，**禁止**字符串拼接
- 异常日志必须含 traceId + 完整堆栈；`log.error("xxx failed", e)` 格式
- 敏感信息脱敏：手机号 `138****8001`、身份证 `3201****1234`、密码/银行卡**禁止**输出

#### 5.2 异常

- 业务异常：`throw new BizException("USER_NOT_FOUND", "用户不存在")`
- 全局处理：`@RestControllerAdvice` + `@ExceptionHandler`，转换 `BizException` → `Result.fail`
- **禁止** Controller 内 `try-catch` 吞掉异常

#### 5.3 实体类

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | `bigint` | 主键（自增或雪花算法） |
| `create_time` | `datetime` | 默认 `CURRENT_TIMESTAMP` |
| `update_time` | `datetime` | 自动更新 |
| `is_deleted` | `tinyint` | 逻辑删除标记（0/1） |

#### 5.4 POJO 类型边界

| 类型 | 用途 | 命名 |
|------|------|------|
| 实体（数据库） | 与表对应 | `XxxPO`（MP 默认）或 `Xxx`（带 `@TableName`） |
| BO | 业务对象（Service 内部） | `XxxBO` |
| DTO | 数据传输（Controller 入参） | `XxxDTO`（带 `@Valid` 校验） |
| VO | 视图对象（Controller 出参） | `XxxVO` |

**禁止** Controller 直接返 `Map<String, Object>` 或裸实体。

#### 5.5 配置管理

- 配置中心选型：见 [08-技术选型与决策记录（ADR）](.dev/docs/2.%20设计阶段/08-技术选型与决策记录（ADR）.md)
- 敏感信息（DB 密码 / 支付密钥）走所选配置中心加密 + KMS，**禁止**写 `application.yml` 明文
- Profile 文件：`application-{profile}.yml`，公共部分放 `application.yml`

### 6. 测试约定

依据《阿里规约 黄山版》制定的质量目标（CI 门禁强制）：

| 指标 | 要求 |
|------|------|
| 单元测试覆盖率 | 核心/非核心模块**行覆盖率 ≥ 80%**；增量代码**行覆盖率 ≥ 85%**（JaCoCo 统计） |
| P3C 扫描 | 无**阻断**级别问题；严重/重要问题清零或经评审豁免 |
| SonarQube 质量门 | 通过默认质量门，新增代码无 Bug/漏洞/异味；重复率 ≤ 3% |

其余分层 / 命名 / CI 卡点 / 自动化见 [01-测试策略文档](.dev/docs/4.%20测试阶段/01-测试策略文档.md)。

## 相关文档链接

> **加载策略**：
> - 本文件正文 = 始终在上下文，速查够用时**无需展开**任何链接
> - 下方表格 = **唯一**导航；不在表中的文档**视为不存在**
> - **按当前任务**在表里找到对应行，**只加载那一行指向的文档**，其他不读
> - 表格内容已自审覆盖所有 Java 后端实现场景；缺哪条 → 提 PR 补表

### 加载触发矩阵

| 当前任务 | 触发加载 |
|----------|----------|
| 写 Java 业务代码（命名/异常/日志/并发/POJO 风格） | [01-Java编码规范](.dev/docs/3.%20开发阶段/01-Java编码规范.md) |
| 启动 / 配置 / Bean / Controller / AOP | [02-SpringBoot使用规范](.dev/docs/3.%20开发阶段/02-SpringBoot使用规范.md) |
| 写 Mapper / 改 Service / 改 Manager / SQL 调优 | [03-MyBatis-Plus规范](.dev/docs/3.%20开发阶段/03-MyBatis-Plus规范.md) |
| 新建模块 / 改包结构 / 拆 Maven 多模块 | [04-项目目录结构规范](.dev/docs/3.%20开发阶段/04-项目目录结构规范.md) |
| 新成员搭建本地环境 | [05-开发环境搭建指南](.dev/docs/3.%20开发阶段/05-开发环境搭建指南.md) |
| 写提交 / 切分支 / 提 PR | [06-Git 提交规范](.dev/docs/3.%20开发阶段/06-Git%20提交规范.md) |
| 评审别人的 PR | [07-代码评审标准](.dev/docs/3.%20开发阶段/07-代码评审标准.md) |
| 改 `Result` / `@Manager` / Lombok / 工具类 | [08-公共组件与工具使用说明](.dev/docs/3.%20开发阶段/08-公共组件与工具使用说明.md) |
| 改架构 / 评估技术选型 | [01-系统架构设计文档](.dev/docs/2.%20设计阶段/01-系统架构设计文档.md) |
| 改数据库表 / 写 SQL / 设计索引 | [03-数据库设计文档](.dev/docs/2.%20设计阶段/03-数据库设计文档.md) |
| 改安全 / 认证 / 鉴权 / 加密 | [04-安全设计文档](.dev/docs/2.%20设计阶段/04-安全设计文档.md) |
| 写测试 / 调测试覆盖率 | [01-测试策略文档](.dev/docs/4.%20测试阶段/01-测试策略文档.md) |
| 写测试用例 | [02-测试用例文档](.dev/docs/4.%20测试阶段/02-测试用例文档.md) |
| 写自动化测试脚本 | [04-自动化测试规范](.dev/docs/4.%20测试阶段/04-自动化测试规范.md) |
| 改 CI/CD / 部署 / 镜像 | [01-构建与部署文档（CI/CD）](.dev/docs/5.%20部署阶段/01-构建与部署文档（CI-CD）.md) |
| 改环境配置 / Profile 差异 | [04-环境配置说明](.dev/docs/5.%20部署阶段/04-环境配置说明.md) |
| 改日志 / 监控 / 链路追踪 | [05-日志与监控约定](.dev/docs/5.%20部署阶段/05-日志与监控约定.md) |
| 排查线上故障 | [08-故障排查手册](.dev/docs/5.%20部署阶段/08-故障排查手册.md) |
| 写文档 / 改文档结构 | [02-文档编写规范](.dev/docs/6.%20项目管理/02-文档编写规范.md) |
| 跨文档术语不统一 | [03-术语表](.dev/docs/6.%20项目管理/03-术语表.md) |

### 何时**不**加载

- 简单改一行 / 改文案 / 改注释 → **不加载**任何文档
- 任务与「Java 后端实现」无关（纯前端 / 纯运维 / 纯脚本） → **不加载**本节
- 当前任务在矩阵中**找不到对应行** → 先问 Owner 是否真的需要改这块，**不擅自加载未列出文档**