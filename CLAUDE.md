# CLAUDE.md

薄荷商城（mini_mail_system）的项目级 AI 协作约定。本文件只记录**仓库里真实存在的硬约束 + 当前已踩到的坑**，长篇规范细节按需就地下钻到 `.dev/docs/`。

> **语言**：模型用中文思考并回复。代码 / 注释 / 提交信息保持英文。
> **RTK Token 优化**：本仓库已启用 [RTK](https://github.com/rtk-ai/rtk) 作 Bash 代理（用户级 settings.json 钩子），所有 `git / mvnw / ls / cat / find / grep` 自动走 RTK，**不要**手动拼前缀。文件读取走专用工具（`Read/Glob/Grep/Edit/Write`），**禁止**用 `cat` 偷懒。详细说明见用户级 `~/.claude/CLAUDE.md`。

---

## 1. 项目快照（30 秒读懂）

| 字段 | 取值 |
|------|------|
| 技术栈 | Java 17 · Spring Boot 3.2 · Spring Cloud Alibaba · MyBatis-Plus 3.5 · Maven 3.8.6 |
| 中间件 | MySQL 8 · Redis 7.2 · RocketMQ 5 · Elasticsearch 8 · Nacos 2.3 · Seata 2 · Sentinel · SkyWalking 9 |
| 包前缀 | `com.yirancrazy.minimall` |
| Profile | `dev`（默认）/ `test`（自动 H2 + 无 Docker）/ `prod`（Nacos + KMS） |
| 启动入口 | Gateway `:8080` 唯一对外；各业务服务各自端口 |

**Maven 模块**（15 个）：

| 类别 | 模块 |
|------|------|
| 脚手架 | `minimall-common`（`BasePO` / `Result` / `BizException` / `EventBus` / `@Manager` / `@Idempotent`） |
| API 契约 | `minimall-api`（跨服务 DTO + FeignClient + FallbackFactory，**禁止**反向依赖任何服务） |
| 网关 | `minimall-gateway`（路由 + JWT 验签 + Sentinel + 灰度） |
| 业务服务 | `minimall-auth-service` `:8201` → `auth_db`<br>`minimall-user-service` `:8202` → `user_db`<br>`minimall-merchant-service` `:8203` → `merchant_db`<br>`minimall-goods-service` `:8204` → `goods_db`<br>`minimall-cart-service` `:8205` → `cart_db`<br>`minimall-order-service` `:8206` → `order_db`<br>`minimall-pay-service` `:8207` → `pay_db`<br>`minimall-stock-service` `:8208` → `stock_db`<br>`minimall-notify-service` `:8209` → `notify_db`<br>`minimall-platform-service` `:8210` → `platform_db`<br>`minimall-id-service`（分布式 ID） |
| 工具 | `minimall-flyway-core`（多服务复用的 Flyway 迁移包） |

**依赖方向（强约束）**：

```
业务服务 ──▶ minimall-api ──▶ minimall-common
   │                              ▲
   └──────────────────────────────┘
common / api 禁反向依赖任何服务；服务间 只能 走 Feign（client 在 minimall-api/feign/）
```

**核心链路**：下单 = Gateway → `order-service` 启 Seata AT → 调 `stock-service` 锁库存 → `pay-service` 创建支付流水 → 支付宝异步回调 → 事务消息 `ORDER_PAID` → `notify-service` 推动站内信 / `order-service` 推进状态。

---

## 2. 常用命令

```bash
# 构建（跳过测试）
./mvnw clean package -DskipTests

# 跑全量单测（自动切 test profile + H2）
./mvnw test

# 跑单个服务 / 某个测试类 / 某个方法
./mvnw -pl minimall-<svc>-service test
./mvnw -pl minimall-order-service test -Dtest=OrderServiceImplTest
./mvnw -pl minimall-order-service test -Dtest=OrderServiceImplTest#create

# 启动单个服务（默认 dev）
./mvnw -pl minimall-order-service spring-boot:run

# 切 profile
./mvnw -pl minimall-order-service spring-boot:run -Dspring-boot.run.profiles=prod

# 静态扫描（CI 卡门）
./mvnw checkstyle:check && ./mvnw com.github.spotbugs:spotbugs-maven-plugin:check && ./mvnw org.owasp:dependency-check-maven:check
```

提交前主动跑 `rtk git status` / `rtk git diff` 复核变更。**不要**等用户提醒。

---

## 3. 提交信息规范

仓库统一使用中文 commit 信息，规范全在 CLAUDE.md 内，**不再另立文档**。

### 3.1 标题格式

格式：`类型(范围): 描述`

- **类型（必选，动名词）**：见表 1
- **范围（可选）**：
  - 业务子域用中文：订单 / 商品 / 用户 / 库存 / 支付 / 商户 / 通知 / 认证
  - 技术子系统名保留英文：gitignore / eol / pom / jwt / api / gateway / ci
- **描述**：≤ 50 字，一个完整动宾短语，主语隐含为本仓库

**表 1 — 中文类型映射**

| 中文类型 | 语义等价 | 中文类型 | 语义等价 |
|----------|----------|----------|----------|
| 新增     | feat     | 测试     | test     |
| 修复     | fix      | 杂项     | chore    |
| 重构     | refactor | 回滚     | revert   |
| 文档     | docs     | 构建     | build    |
| 格式     | style    | 优化     | perf     |
| 流水线   | ci       |          |          |

### 3.2 body 排版

- 标题与 body 之间**空一行**
- 一项改动可用一两句散文说明
- ≥ 3 处实质改动时**强制**用 `- 一行改动摘要` 的 bullet 列表
- 代码块 / 命令用 Markdown ` ``` ` 围栏
- 段落之间空一行，body 不限总长度

### 3.3 禁词库

下列内容禁止出现在 commit 标题或 body 中：

- **迭代标记**：`iter` / `iter-1` / `iter-7` / `Iter-6` 等任何带 `iter` 的字样
- **源码外文档路径**：`.dev/docs/...` 字面路径，及其下任一文件名（如 `01-Java编码规范.md`）
- **指向源码外文档的词**：`开发规范` / `编码规范` / `设计文档` / `API 文档` 等
- **英文文档术语标记**：`§` / `¶` / `Chapter` / `Section` / `Article` / `Appendix` / `Sec.`
- **中文章节序号**：`第 N 章` / `第一节` / `第 N 条` / `第一项` 等

原因：commit 是不可变快照，反向引用外部文档会让历史快照与文档版本脱钩。

### 3.4 样例

**新增 + 业务域中文 scope**（含 bullet body）

```
新增(订单): 引入 OrderExpireScheduler，定时关单

- 新增 30 分钟超时关单调度器
- 复用 OrderService.cancel 的状态校验
- 联动库存释放，自动触发 StockService.release
```

**修复 + 技术英文 scope**

```
修复(库存): reserve 在 quantity≤0 时不再抛 NPE
```

**重构 + 业务域中文 scope**

```
重构(订单): 用 EventBus 抽象替换 LocalEventBus 具体类型
```

**杂项 + 技术英文 scope**

```
杂项(gitignore): 排除本地脚本不入库

将 7 个本地辅助脚本从仓库中剔除：
- 启动 / 停止开发环境的 shell 脚本
- 多阶段验证脚本
- Javadoc 占位文本重写器与换行符规范化器

scripts/ 目录及全部文件保留在硬盘上，仅本机使用。
```

---

## 4. 代码现实（2026-07-29 盘点）

**这节是为了让规则贴近代码，不是粉饰。** 读这一节能立刻知道哪些是"理想规范"、哪些是"现状就这么写"。

### 4.1 分层（实际落地版）

```
<svc>-service/src/main/java/com/yirancrazy/minimall/<svc>/
├── <Svc>Application.java
├── config/                    # SchemaConfig / OpenAPI / 业务相关 Bean
├── controller/v1/
│   ├── XxxControllerV1.java           # 对外：/api/v1/<svc>
│   └── InternalXxxControllerV1.java   # 服务间：/internal/<svc>（Gateway 需配白名单）
├── service/                   # Service 接口（薄业务层；impl 在 impl/）
│   ├── XxxService.java
│   └── impl/XxxServiceImpl.java
├── manager/                   # 跨 Mapper / 跨聚合
│   ├── XxxManager.java                # extends IService<T>
│   └── impl/XxxManagerImpl.java       # extends ServiceImpl<M,T> + @Manager
├── mapper/XxxMapper.java              # extends BaseMapper<T>
├── entity/XxxPO.java                  # extends BasePO（自带 id/createTime/updateTime/isDeleted）
├── constant/                           # XxxCodeEnum（按需，不是每个服务都有）
├── dto/ vo/                            # 仅 auth-service 落地，其余服务直接用 PO
├── listener/ sse/ util/                # 按需（notify 用 listener+sse；auth 用 util/JwtUtil）
└── 禁：feign/                          # 服务间调用走 minimall-api/feign，不在服务内自建
```

⚠️ **CLAUDE.md 历史版本写过 `bo/` 目录**——仓库里**0 个**，新代码**不要**创建。

### 4.2 ✅ 已遵守的规则（不要回退）

| 规则 | 证据 |
|------|------|
| Service 薄业务层 | `XxxServiceImpl` 只组合 Manager + Feign，不直连 Mapper |
| Manager 继承 `IService<T>` + `ServiceImpl<M,T>` | 9 个 Manager 全部符合 |
| Controller 类名带 `V1` 后缀 | 全部符合 |
| 内部接口走 `/internal/<svc>` + `InternalXxxControllerV1` | 10 个服务全部落地 |
| 跨服务走 `minimall-api/feign/` | 10 个 FeignClient 全部在 api 模块 |
| 业务异常用 `BizException` | `GlobalExceptionHandler` 统一转码 |
| 事件总线 `EventBus` 抽象 | `LocalEventBus`（默认）+ `RocketMqEventBus`（prod profile） |
| BasePO 自带 `id/createTime/updateTime/isDeleted` | 全部 PO 继承 |
| `@TableLogic` 逻辑删除 | 由 BasePO 全局开启 |
| Controller 禁止 try-catch | 全部符合 |

### 4.3 ⚠️ 实际没遵守的规则（要修）

| 规则 | 实际差距 | 修法 |
|------|----------|------|
| Service / Controller 必须用 VO/DTO，**禁止**裸 PO 返回 | `CartService.listByUser` / `NotifyService.listByUser` 直接返 `List<PO>` | 补 `XxxVO` + 映射（推荐 `MapStruct`，未引入则手写静态工厂） |
| 入参 `@Valid` | 仅 `AuthControllerV1` 用了；`Cart` / `Order` / `Pay` 缺 | 加 `@Valid @RequestBody XxxDTO` + DTO 字段 `@NotBlank/@Min/...` |
| Service 必须 `@Slf4j` + 关键路径日志 | 仅 `OrderServiceImpl` 用了；`Cart` / `Goods` / `Pay` / `Stock` / `User` / `Merchant` / `Notify` / `Auth` 都没 | 类上补 `@Slf4j`；`log.info("user={}, action={}", userId, "reserve")` |
| 业务层 throw `BizException(XxxCodeEnum.ALIAS)` | `CartServiceImpl` / `NotifyService` 0 BizException | 业务失败统一抛（`GlobalExceptionHandler` 已配好） |
| 状态字段用 enum（`code, alias, message`） | `OrderPO.status` / `PayRecordPO.status` 用 `String "PENDING/PAID/FAILED"` | 改 `OrderStatusEnum` / `PayStatusEnum`，存 `enum.name()` |
| 单元测试行覆盖率 ≥ 80% | 核心服务 `Cart/Order/Pay/Stock` 几乎 0 单测 | 给每个 `XxxServiceImpl` 补 Mockito 单测（happy + 失败 + 边界） |
| 工具链 SpotBugs / Checkstyle / OWASP | 未跑过 | CI 阶段补 |

---

## 5. 硬约束（必须遵守，违反直接拒绝合入）

### 5.1 命名 & 分层

- **Controller**：`XxxControllerV1`（对外）/ `InternalXxxControllerV1`（服务间，`/internal/<svc>`），统一返 `Result<T>`
- **Service**：`XxxService` 接口 + `XxxServiceImpl`（**禁止**继承 `IService` / `ServiceImpl`）
- **Manager**：`XxxManager` extends `IService<T>` + `XxxManagerImpl` extends `ServiceImpl<M,T>`，类上**只打 `@Manager`**（它本身是 `@Service` 元注解，**不要再叠加 `@Service`**）
- **POJO 边界**：`XxxPO`（库表）/ `XxxDTO`（入参，加 `@Valid`）/ `XxxVO`（出参）。**没有 `bo/`**。跨服务传输用 `minimall-api/dto/XxxDTO`
- **枚举**：放 `constant/`，字段 `(code, alias, message)`，实现 `com.yirancrazy.minimall.common.base.BaseEnum`
- **实体类**：必须继承 `BasePO`（自带 `id / createTime / updateTime / isDeleted`）
- **API 路径**：业务服务不暴露非 `V1` 的 Controller 类

### 5.2 统一返回 & 异常

- 用 `Result.success(data)` / `Result.fail(code, msg)` 构造；code 遵循 `00000` 成功 / `1xxxx` 业务错误 / `2xxxx` 系统错误
- **业务异常**：`throw new BizException(XxxCodeEnum.ALIAS)`，**禁止**在 Service 返回 error 码（`return false` / `-1` / `null` 等）
- **Controller 禁止 try-catch**——`GlobalExceptionHandler`（在 `minimall-common`）统一抓 `BizException / BaseException / MethodArgumentNotValidException / MissingRequestHeaderException / Throwable`
- 任何未知异常回到 `CommonCode.SYS_ERROR`（"系统繁忙"），**禁止**在 Controller 里 `try { ... } catch (Exception e) { return Result.fail(...) }`

### 5.3 日志

- 所有业务类（Service / Manager / Listener / Consumer）加 `@Slf4j`
- 占位符：`log.info("user={}, skuId={}", userId, skuId)`，**禁止**字符串拼接
- 异常日志：`log.error("xxx failed", e)`（带完整堆栈）
- 敏感字段脱敏：手机号 `138****8001` / 身份证 `3201****1234`；密码、银行卡、盐、Token **禁止**落日志

### 5.4 依赖方向

- `minimall-common` / `minimall-api` **禁止**反向依赖任何服务模块
- 业务服务之间**只能**通过 `minimall-api/feign/XxxFeignClient` + `XxxFeignFallbackFactory` 通信
- 不允许在 Service / Manager 里直接 new HttpClient / RestTemplate 调别的服务

### 5.5 安全 & 配置

- DB 密码 / 支付密钥 / KMS 走 Nacos `application-prod.yml` 加密配置，**禁止**明文写 `application.yml`
- 本地 dev 用 `application-dev.yml`；测试自动 `application-test.yml` + H2（无需 Docker）
- 配置文件 deny 规则见 `.claude/settings.json`（已 deny `application-prod.yaml`、`application-dev.yaml`、`.env`、`*.key/*.pem` 等）

---

## 6. 反模式检查清单（来自真实代码）

> 新代码合入前过一遍；命中任意一项 → 改完再提。

**6.1 🚫 全局 Javadoc 模板（IDE 自动生成，仓库里 100+ 处）**

```java
/**
 * @Author: yirancrazy@gmail.com
 * @Description: Xxx 类（待人工补充准确描述）。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
```

**禁止**写、**禁止**复制粘贴。已有类用一次性 PR 删干净。同时**改 IDE Live Template**（IntelliJ：`Settings → Editor → File and Code Templates → Class`），把头 4 行删掉。

**6.2 🚫 空的 Javadoc 占位**

```java
/**
 * TODO(spec): describe xxx contract.
 * @param ... TODO
 * @return ... TODO
 */
```

**禁止**写、**禁止**复制粘贴。

**6.3 🚫 Service 双重注解**

```java
@Service
@Manager
public class XxxManagerImpl extends ServiceImpl<M, T> implements XxxManager { }
```

`@Manager` 本身已 `@Service`，**只保留 `@Manager`**，否则 Spring 容器可能出现重复 Bean 警告。

**6.4 🚫 String 状态字段**

```java
po.setStatus("PENDING_PAY");   // ❌
po.setStatus("PENDING");       // ❌
```

应改为 `enum`：定义 `OrderStatusEnum { PENDING_PAY, PAID, FAILED, REFUNDED }`，存 `po.setStatus(OrderStatusEnum.PENDING_PAY.name())`。

**6.5 🚫 裸 PO 进出 Controller**

```java
// Service
List<CartItemPO> listByUser(Long userId);

// Controller
public Result<List<CartItemPO>> list(...) { ... }  // ❌ 直接暴露数据库结构
```

应改为 `List<CartItemVO>`，PO → VO 的映射在 `XxxServiceImpl` 里写静态工厂或 `MapStruct`。

**6.6 🚫 路径参数 / `@RequestParam` 拼业务入参**

```java
@PostMapping
public Result<Long> create(@RequestParam("userId") Long userId,
                           @RequestParam("skuId") Long skuId,
                           @RequestParam("quantity") Integer quantity) { ... }  // ❌
```

应改为：

```java
@Data public class OrderCreateDTO {
    @NotNull private Long userId;
    @NotNull private Long skuId;
    @Min(1)  private Integer quantity;
}
@PostMapping
public Result<Long> create(@Valid @RequestBody OrderCreateDTO dto) { ... }
```

**6.7 🚫 业务路径用 `@RequestParam` 携带身份字段**

`userId / userName / role` 取自 JWT claim，**禁止**从前端 `@RequestParam` 拼。同理所有 `InternalXxxControllerV1` 调用方必须从 `SecurityContext` / Feign Header 注入身份，**禁止** `?userId=xxx`。

**6.8 🚫 Service 吞异常 / 业务失败返回布尔**

```java
if (!reserved) return false;  // ❌ 吞掉语义
```

应：`throw new BizException(OrderCodeEnum.STOCK_RESERVE_FAIL);` —— 由 `GlobalExceptionHandler` 转 `Result.fail("11001", "库存锁定失败")`。

**6.9 🚫 NotifyService 写法（仅此一处不一致）**

`NotifyService` 是裸 class，没有接口、没有 `service/impl/` 分离。**改**成 `NotifyService` interface + `NotifyServiceImpl` impl，对齐其他服务。

**6.10 🚫 `Internal*` 路径无鉴权**

目前 `/internal/*` 直接裸暴露。Gateway 必须在 `application.yml` + 路由白名单里限定内部 IP 段，**不要**给公网开。

---

## 7. 测试约定

| 频率 | 要求 |
|------|------|
| Service 单测 | 强制：每个 `XxxServiceImpl` 至少 1 个 Mockito 单测，覆盖 happy + 失败 + 边界 |
| Controller 单测 | 推荐：MockMvc + `@WebMvcTest` |
| 集成测试 | `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`（已有范例：`CartControllerV1Test`） |
| 覆盖率 | 核心 / 非核心模块行覆盖率 ≥ 80%；增量代码 ≥ 85%（JaCoCo 统计；先补单测，再谈门禁） |
| Mock 中间件 | 测试 profile 自动 H2 + 不依赖 Docker；`minimall-auth-service` 已用 `H2SchemaConfig` 演示 |

---

## 8. 加载触发矩阵（按需就地下钻）

> **规则**：表中没有的文档**视为不存在**，**不要**擅自加载。

| 当前任务 | 加载 |
|----------|------|
| 写 Java 业务代码（命名 / 异常 / 日志 / 并发 / POJO 风格） | [.dev/docs/3. 开发阶段/01-Java编码规范.md](.dev/docs/3.%20开发阶段/01-Java编码规范.md) |
| 启动 / 配置 / Bean / Controller / AOP | [.dev/docs/3. 开发阶段/02-SpringBoot使用规范.md](.dev/docs/3.%20开发阶段/02-SpringBoot使用规范.md) |
| 写 Mapper / 改 Service / 改 Manager / SQL 调优 | [.dev/docs/3. 开发阶段/03-MyBatis-Plus规范.md](.dev/docs/3.%20开发阶段/03-MyBatis-Plus规范.md) |
| 新建模块 / 改包结构 / 拆 Maven 多模块 | [.dev/docs/3. 开发阶段/04-项目目录结构规范.md](.dev/docs/3.%20开发阶段/04-项目目录结构规范.md) |
| 新成员搭建本地环境 | [.dev/docs/3. 开发阶段/05-开发环境搭建指南.md](.dev/docs/3.%20开发阶段/05-开发环境搭建指南.md) |
| 写提交 / 切分支 / 提 PR | [.dev/docs/3. 开发阶段/06-Git 提交规范.md](.dev/docs/3.%20开发阶段/06-Git%20提交规范.md) |
| 评审别人的 PR | [.dev/docs/3. 开发阶段/07-代码评审标准.md](.dev/docs/3.%20开发阶段/07-代码评审标准.md) |
| 改 `Result` / `@Manager` / Lombok / 工具类 | [.dev/docs/3. 开发阶段/08-公共组件与工具使用说明.md](.dev/docs/3.%20开发阶段/08-公共组件与工具使用说明.md) |
| 改架构 / 评估技术选型 | [.dev/docs/2. 设计阶段/01-系统架构设计文档.md](.dev/docs/2.%20设计阶段/01-系统架构设计文档.md) |
| 改 API / 写 OpenAPI | [.dev/docs/2. 设计阶段/02-API设计文档.md](.dev/docs/2.%20设计阶段/02-API设计文档.md) |
| 改数据库表 / 写 SQL / 设计索引 | [.dev/docs/2. 设计阶段/03-数据库设计文档.md](.dev/docs/2.%20设计阶段/03-数据库设计文档.md) |
| 改安全 / 认证 / 鉴权 / 加密 | [.dev/docs/2. 设计阶段/04-安全设计文档.md](.dev/docs/2.%20设计阶段/04-安全设计文档.md) |
| 写测试 / 调测试覆盖率 | [.dev/docs/4. 测试阶段/01-测试策略文档.md](.dev/docs/4.%20测试阶段/01-测试策略文档.md) |
| 写测试用例 | [.dev/docs/4. 测试阶段/02-测试用例文档.md](.dev/docs/4.%20测试阶段/02-测试用例文档.md) |
| 写自动化测试脚本 | [.dev/docs/4. 测试阶段/04-自动化测试规范.md](.dev/docs/4.%20测试阶段/04-自动化测试规范.md) |
| 改 CI/CD / 部署 / 镜像 | [.dev/docs/5. 部署阶段/01-构建与部署文档（CI-CD）.md](.dev/docs/5.%20部署阶段/01-构建与部署文档%28CI-CD%29.md) |
| 改环境配置 / Profile 差异 | [.dev/docs/5. 部署阶段/04-环境配置说明.md](.dev/docs/5.%20部署阶段/04-环境配置说明.md) |
| 改日志 / 监控 / 链路追踪 | [.dev/docs/5. 部署阶段/05-日志与监控约定.md](.dev/docs/5.%20部署阶段/05-日志与监控约定.md) |
| 排查线上故障 | [.dev/docs/5. 部署阶段/08-故障排查手册.md](.dev/docs/5.%20部署阶段/08-故障排查手册.md) |
| 写文档 / 改文档结构 | [.dev/docs/6. 项目管理/02-文档编写规范.md](.dev/docs/6.%20项目管理/02-文档编写规范.md) |
| 跨文档术语不统一 | [.dev/docs/6. 项目管理/03-术语表.md](.dev/docs/6.%20项目管理/03-术语表.md) |

**不加载的情形**：

- 简单改一行 / 改文案 / 改注释 → 不加载任何文档
- 任务与"Java 后端实现"无关（纯前端 / 纯运维 / 纯脚本）→ 不加载本节
- 矩阵中找不到的行 → 先问 Owner 是否真的需要改这块，**不要**擅自加载未列出文档

---

## 9. 已知待清理清单（下一波迭代）

> 这些是 §4.3 的衍生 Action Item，按模块分桶追踪。一个 PR 一个桶。

- [ ] **cart-service**：补 `dto/vo/` + 全部 Service 加 `@Slf4j` + 用 `BizException` 替换所有 `return false`
- [ ] **order-service / pay-service / stock-service**：`status` 字段 enum 化 + Controller 入参改 DTO
- [ ] **goods / user / merchant / notify / id / platform**：补单测（Mockito 单测 + 边界）
- [ ] **全仓库**：删除「`@Author / @Description / @Version / @DateTime`」4 行 Javadoc 模板（一次性 PR；同时改 IDE Live Template）
- [ ] **全仓库**：删除「`TODO(spec): describe xxx contract.`」空 Javadoc（一次性 PR）
- [ ] **NotifyService**：拆 interface + impl，对齐其他服务
- [ ] **Gateway + 全服务**：补 `/internal/*` 路由白名单
- [ ] **CI**：接入 `checkstyle` / `spotbugs` / `owasp` 门禁（脚本命令已在 §2 给出）

---

## 10. 协作约定

- **功能设计 / 创意发散** → 走 `brainstorming` skill
- **设计落地 / 跨文件决策 / 本文件改动** → 走 `grilling` skill
- 改代码前先用 `Glob` / `Grep` 定位相关文件，**只读必要的几个**，不要一次性铺开
- 跨模块改动 / 根因不明的 bug：先看目录结构 + 调一遍调用链，再动手；别只盯单个文件
- 方案设计类任务（新功能、重构）：先理清模块边界再写
- 报告完成 / 跑测 / 构建 / 提交前，**主动**通过 `rtk git status` 与 `rtk git diff` 复核变更
- 提交信息遵循本文档 §3 《提交信息规范》（**优先于**仓库内任何外部研发资料）
