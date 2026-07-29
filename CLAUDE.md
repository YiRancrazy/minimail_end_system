# CLAUDE.md

薄荷商城（mini_mail_system）的项目级 AI 协作约定。本文件只记录**仓库事实、高频硬约束和已踩坑**；细节按任务从 `.dev/docs/` 加载。

> **语言**：模型用中文思考并回复；代码与注释用英文；提交信息按本文第 3 节使用中文。
> **规则优先级**：直接用户要求 > 本文件 > 按需加载的项目文档。发现冲突时先指出，不自行扩大修改范围。
> **RTK**：Bash 命令由用户级 Hook 自动代理，禁止手动添加 `rtk` 前缀。读写文件优先用 `Read/Glob/Grep/Edit/Write`，禁止用 `cat/find/grep` 代替专用工具。

---

## 1. 项目快照

| 字段 | 取值 |
|------|------|
| 技术栈 | Java 17 · Spring Boot 3.2 · Spring Cloud Alibaba · MyBatis-Plus 3.5 · Maven 3.8.6 |
| 中间件 | MySQL 8 · Redis 7.2 · RocketMQ 5 · Elasticsearch 8 · Nacos 2.3 · Seata 2 · Sentinel · SkyWalking 9 |
| 包前缀 | `com.yirancrazy.minimall` |
| Profile | `dev`（默认）/ `test`（H2、无 Docker）/ `prod`（Nacos + KMS） |
| 外部入口 | Gateway `:8080`；业务服务不直接对公网暴露 |

**Maven 模块（15 个）**：

- 基础：`minimall-common`、`minimall-api`、`minimall-gateway`
- 业务：`auth :8201`、`user :8202`、`merchant :8203`、`goods :8204`、`cart :8205`、`order :8206`、`pay :8207`、`stock :8208`、`notify :8209`、`platform :8210`
- 其他：`minimall-id-service`、`minimall-flyway-core`

**依赖方向（强约束）**：

```text
业务服务 ──▶ minimall-api ──▶ minimall-common
   │                              ▲
   └──────────────────────────────┘
```

- `common`、`api` 禁止反向依赖服务模块。
- 服务间通信只能走 `minimall-api/feign/` 中的 FeignClient。
- 当前仓库有 10 个 ManagerImpl、9 个 FeignClient、8 个 Internal Controller；数量是现状，不是新增模块的目标值。

**核心链路**：Gateway → `order-service` 启动 Seata AT → `stock-service` 锁库存 → `pay-service` 创建支付流水 → 支付宝回调 → 事务消息 `ORDER_PAID` → `notify-service` 通知并由 `order-service` 推进状态。

---

## 2. 常用命令

```bash
# 构建（跳过测试）
./mvnw clean package -DskipTests

# 全量 / 单模块 / 单类 / 单方法测试
./mvnw test
./mvnw -pl minimall-order-service test
./mvnw -pl minimall-order-service test -Dtest=OrderServiceImplTest
./mvnw -pl minimall-order-service test -Dtest=OrderServiceImplTest#create

# 启动服务；prod 仅在明确需要时使用
./mvnw -pl minimall-order-service spring-boot:run
./mvnw -pl minimall-order-service spring-boot:run -Dspring-boot.run.profiles=prod

# CI 静态检查
./mvnw checkstyle:check
./mvnw com.github.spotbugs:spotbugs-maven-plugin:check
./mvnw org.owasp:dependency-check-maven:check
```

完成修改、跑测、构建或提交前，主动运行 `git status --short` 和 `git diff` 复核；不要等用户提醒。

---

## 3. 提交信息规范

仓库提交信息统一使用中文，格式为：

```text
类型(范围): 完整动宾短语
```

- **类型**：`新增`、`修复`、`重构`、`文档`、`格式`、`测试`、`杂项`、`回滚`、`构建`、`优化`、`流水线`。
- **范围**：业务域用中文（订单、商品、用户、库存、支付、商户、通知、认证）；技术子系统保留英文（api、gateway、jwt、pom、ci、eol、gitignore）。
- **标题**：不超过 50 字；标题和正文之间空一行。
- **正文**：一两处改动可用散文；至少 3 处实质改动必须使用 bullet 列表。
- 未经用户明确要求，不创建提交、不推送、不提 PR。

**提交禁词**：

- 任何带 `iter` 的迭代标记。
- `.dev/docs/...`、其中的文件名，以及“开发规范、编码规范、设计文档、API 文档”等反向引用。
- `§`、`¶`、`Chapter`、`Section`、`Article`、`Appendix`、`Sec.`。
- “第 N 章、第一节、第 N 条、第一项”等章节序号。

原因：提交是不可变快照，不应依赖会继续演化的外部文档位置。

**示例**：

```text
新增(订单): 引入超时关单调度器

- 新增 30 分钟超时关单任务
- 复用订单取消的状态校验
- 联动释放锁定库存
```

---

## 4. 分层与代码边界

```text
<svc>-service/src/main/java/com/yirancrazy/minimall/<svc>/
├── <Svc>Application.java
├── config/
├── controller/v1/
│   ├── XxxControllerV1.java
│   └── InternalXxxControllerV1.java      # 按需，路径 /internal/<svc>
├── service/
│   ├── XxxService.java
│   └── impl/XxxServiceImpl.java
├── manager/
│   ├── XxxManager.java                   # extends IService<T>
│   └── impl/XxxManagerImpl.java          # extends ServiceImpl<M,T> + @Manager
├── mapper/XxxMapper.java                 # extends BaseMapper<T>
├── entity/XxxPO.java                     # extends BasePO
├── constant/                             # enum / code enum，按需
├── dto/ vo/                              # 服务边界对象，按需
└── listener/ consumer/ sse/ util/        # 按需
```

- **Controller**：外部类名 `XxxControllerV1`，内部类名 `InternalXxxControllerV1`；统一返回 `Result<T>`。
- **Service**：接口与实现分离；禁止继承 `IService` / `ServiceImpl`，禁止直接调用 Mapper 或编排多个 Mapper。
- **Manager**：接口继承 `IService<T>`，实现继承 `ServiceImpl<M,T>`；实现类只加 `@Manager`，不得叠加 `@Service`。
- **POJO**：`XxxPO` 仅用于持久化，`XxxDTO` 用于入参，`XxxVO` 用于出参；跨服务 DTO 放在 `minimall-api/dto/`。
- **禁止新增 `bo/`**：仓库当前没有 BO 层；即使按需文档仍出现旧目录，也以本条为准。
- **禁止在服务模块自建 `feign/`**：FeignClient 与 FallbackFactory 统一放在 `minimall-api/feign/`。
- **实体**：必须继承 `BasePO`，复用 `id/createTime/updateTime/isDeleted`；数据库列由驼峰转下划线映射。
- **枚举**：放在 `constant/`，业务枚举字段为 `(code, alias, message)` 并实现 `BaseEnum`；持久化状态使用 `code(int)`，不得用裸字符串或 `enum.name()` 作为新方案。

---

## 5. 返回、异常与接口

- 使用 `Result.success(data)` / `Result.fail(code, message)`；成功码为 `00000`。
- 业务错误码按服务分段，系统错误使用 `2xxxx`；分段细节按任务加载公共组件文档，禁止随意占号。
- 业务失败必须 `throw new BizException(XxxCodeEnum.SPECIFIC_ERROR)`，不得以 `false`、`-1`、`null` 吞掉语义。
- Controller 禁止 `try-catch`；由 `GlobalExceptionHandler` 处理业务异常、参数异常与未知异常。
- 未知异常统一返回 `CommonCode.SYS_ERROR`（“系统繁忙”），不得暴露堆栈或内部异常文本。
- 请求体使用 `@Valid @RequestBody XxxDTO`，DTO 字段添加具体的 Jakarta Validation 约束。
- 禁止用 `@RequestParam Map` 或多个 `@RequestParam` 拼装业务对象。
- `userId/userName/role` 来自 JWT claim / SecurityContext / 可信 Feign Header，禁止接受前端传入的身份字段。
- 幂等写接口使用仓库已有 `@Idempotent` 机制，并定义稳定的幂等键。

---

## 6. 日志、安全与配置

### 6.1 日志

- Service、Manager、Listener、Consumer 等业务类使用 `@Slf4j`。
- 使用占位符：`log.info("user={}, skuId={}", userId, skuId)`；禁止字符串拼接。
- 异常日志保留完整堆栈：`log.error("reserve stock failed", e)`。
- 手机号、身份证必须脱敏；密码、盐、Token、银行卡号、支付密钥禁止落日志。

### 6.2 安全与配置

- DB 密码、支付密钥、KMS 配置不得明文写入通用配置文件；prod 通过 Nacos 与 KMS 管理。
- 本地使用 `application-dev.yml`，测试使用 `application-test.yml` + H2，不依赖 Docker。
- 受保护配置的 deny 规则以 `.claude/settings.json` 为准；遇到拒绝不要绕过权限。
- `/internal/**` 当前尚未完成可信边界隔离；不得把内部接口暴露给公网，也不得把路径白名单误当鉴权。
- 金额统一使用 `BigDecimal`，禁止使用 `float` / `double`。
- 数据库结构变更使用 Flyway 版本化迁移，不在运行时临时改表。

---

## 7. 当前技术债与禁止回退项

以下内容是仓库现状，不代表新代码可以继续复制：

| 领域 | 当前缺口 | 新代码要求 |
|------|----------|------------|
| DTO / VO | Cart、Notify 等仍暴露 PO | Controller 边界只使用 DTO / VO |
| 参数校验 | 多数 Controller 尚未使用 `@Valid` | 新请求体必须校验 |
| 日志 | 多个 Service 缺少关键路径日志 | 新业务类使用 `@Slf4j` 并记录关键状态 |
| 异常 | Cart、Notify 存在布尔值表达失败 | 失败抛具体 `BizException` |
| 状态 | Order、Pay 等仍有 String 状态 | 新状态使用 code 枚举 |
| 测试 | 核心服务覆盖不足 | 新增逻辑至少覆盖正常、失败、边界 |
| 内部接口 | `/internal/**` 尚未完成可信隔离 | 不新增公网可达的内部接口 |
| 工具链 | Checkstyle、SpotBugs、OWASP 尚未形成完整门禁 | 不引入新的扫描告警 |

**禁止复制的历史写法**：

1. `@Author/@Description/@Version/@DateTime` 类头模板。
2. `TODO(spec): describe ...` 等空 Javadoc。
3. `@Service` 与 `@Manager` 叠加。
4. `po.setStatus("PENDING")` 或 `enum.name()` 持久化新状态。
5. Controller 直接返回 `XxxPO`。
6. Service 以 `return false/null/-1` 表达业务失败。
7. 裸 class 形式的 Service；接口与 `service/impl/` 必须分离。

---

## 8. 测试约定

| 类型 | 要求 |
|------|------|
| Service 单测 | 强制；Mockito 覆盖正常、失败、边界 |
| Controller 单测 | 推荐 `MockMvc` + `@WebMvcTest` |
| 集成测试 | `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` |
| 测试环境 | test profile + H2，不依赖 Docker |
| 覆盖率 | 关键模块 ≥ 85%，普通模块 ≥ 70%；新增代码目标 ≥ 85% |

测试应验证业务结果与异常语义，不为覆盖率编写无断言测试。

---

## 9. 按需加载矩阵

> 只加载当前任务对应的最少文档。矩阵未覆盖的主题先询问 Owner，不擅自扫描无关资料。

| 当前任务 | 加载文档 |
|----------|----------|
| Java 命名、异常、日志、并发、POJO | [.dev/docs/3. 开发阶段/01-Java编码规范.md](.dev/docs/3.%20开发阶段/01-Java编码规范.md) |
| Spring Bean、Controller、AOP、配置 | [.dev/docs/3. 开发阶段/02-SpringBoot使用规范.md](.dev/docs/3.%20开发阶段/02-SpringBoot使用规范.md) |
| Mapper、Service、Manager、SQL | [.dev/docs/3. 开发阶段/03-MyBatis-Plus规范.md](.dev/docs/3.%20开发阶段/03-MyBatis-Plus规范.md) |
| 模块、包结构、Maven 多模块 | [.dev/docs/3. 开发阶段/04-项目目录结构规范.md](.dev/docs/3.%20开发阶段/04-项目目录结构规范.md) |
| 本地环境 | [.dev/docs/3. 开发阶段/05-开发环境搭建指南.md](.dev/docs/3.%20开发阶段/05-开发环境搭建指南.md) |
| Git、分支、PR | [.dev/docs/3. 开发阶段/06-Git 提交规范.md](.dev/docs/3.%20开发阶段/06-Git%20提交规范.md) |
| 代码评审 | [.dev/docs/3. 开发阶段/07-代码评审标准.md](.dev/docs/3.%20开发阶段/07-代码评审标准.md) |
| Result、Manager、公共工具 | [.dev/docs/3. 开发阶段/08-公共组件与工具使用说明.md](.dev/docs/3.%20开发阶段/08-公共组件与工具使用说明.md) |
| 架构或技术选型 | [.dev/docs/2. 设计阶段/01-系统架构设计文档.md](.dev/docs/2.%20设计阶段/01-系统架构设计文档.md) |
| API / OpenAPI | [.dev/docs/2. 设计阶段/02-API设计文档.md](.dev/docs/2.%20设计阶段/02-API设计文档.md) |
| 表、SQL、索引 | [.dev/docs/2. 设计阶段/03-数据库设计文档.md](.dev/docs/2.%20设计阶段/03-数据库设计文档.md) |
| 安全、认证、鉴权、加密 | [.dev/docs/2. 设计阶段/04-安全设计文档.md](.dev/docs/2.%20设计阶段/04-安全设计文档.md) |
| 测试策略或覆盖率 | [.dev/docs/4. 测试阶段/01-测试策略文档.md](.dev/docs/4.%20测试阶段/01-测试策略文档.md) |
| 测试用例 | [.dev/docs/4. 测试阶段/02-测试用例文档.md](.dev/docs/4.%20测试阶段/02-测试用例文档.md) |
| 自动化测试脚本 | [.dev/docs/4. 测试阶段/04-自动化测试规范.md](.dev/docs/4.%20测试阶段/04-自动化测试规范.md) |
| CI/CD、部署、镜像 | [.dev/docs/5. 部署阶段/01-构建与部署文档（CI-CD）.md](.dev/docs/5.%20部署阶段/01-构建与部署文档%28CI-CD%29.md) |
| Profile 与环境配置 | [.dev/docs/5. 部署阶段/04-环境配置说明.md](.dev/docs/5.%20部署阶段/04-环境配置说明.md) |
| 日志、监控、链路追踪 | [.dev/docs/5. 部署阶段/05-日志与监控约定.md](.dev/docs/5.%20部署阶段/05-日志与监控约定.md) |
| 线上故障 | [.dev/docs/5. 部署阶段/08-故障排查手册.md](.dev/docs/5.%20部署阶段/08-故障排查手册.md) |
| 文档结构与写法 | [.dev/docs/6. 项目管理/02-文档编写规范.md](.dev/docs/6.%20项目管理/02-文档编写规范.md) |
| 跨文档术语 | [.dev/docs/6. 项目管理/03-术语表.md](.dev/docs/6.%20项目管理/03-术语表.md) |

**无需加载**：改一行文案或注释、纯前端/运维/脚本任务，以及当前上下文已足以完成的机械修改。

---

## 10. 协作约定

- 功能设计或创意发散先使用 `brainstorming` skill。
- 修改本文件、增删硬约束或进行跨文件设计决策时使用 `grilling` skill。
- 动手前先用 `Glob/Grep` 定位，只读必要文件；跨模块改动先梳理调用链。
- 根因不明的 bug 先复现和定位，不凭猜测修补。
- 不覆盖用户已有改动；发现工作区有无关变更时保留并明确说明。
- 未经要求不创建额外文档、不扩大范围、不提交或推送。
- 完成前必须复核 `git status --short` 与 `git diff`，如实报告未运行的测试或检查。
