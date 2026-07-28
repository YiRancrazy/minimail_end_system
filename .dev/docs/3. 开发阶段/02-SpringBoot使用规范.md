# SpringBoot 使用规范

## 文档信息

| 字段 | 内容 |
|------|------|
| 文档名称 | SpringBoot 使用规范 |
| 文档版本 | V1.0 |$
| 所属阶段 | 开发阶段 |
| 文档状态 | 已发布 |
| 创建人 | yirancrazy@gmail.com |
| 创建日期 | 2026-07-27 |
| 最后更新 | 2026-07-28 |[\$]

## 文档目的

> 统一 Spring Boot 3.x 项目的工程结构、配置、Bean 注入、异常处理等最佳实践。

## 适用范围

- **适用对象**：Java 后端开发
- **适用场景**：新建 Spring Boot 模块、Code Review
- **不适用范围**：Spring 旧版本（5.x 以下）、非 Spring 项目

## 详细内容

### 1. 版本与依赖

| 组件 | 推荐版本 |
|------|----------|
| Spring Boot | 3.2.x |
| Spring Cloud | 2023.0.1.x |
| Java | 17（LTS） |
| Maven | 3.8.6 |
| Lombok | 1.18.30+ |

**关键 Starter 精确 Artifact**（Spring Boot 3.x 专用，禁止使用旧版 starter）：

| Artifact | 版本 | 说明 |
|----------|------|------|
| `com.baomidou:mybatis-plus-spring-boot3-starter` | 3.5.7 | 非 `mybatis-plus-boot-starter`，否则报 `factoryBeanObjectType` |
| `com.alibaba:druid-spring-boot-3-starter` | 1.2.21 | 非 `druid-spring-boot-starter`，否则自动装配失败 |
| `com.xxl-job:xxl-job-core` | 3.0.0 | 2.4.x 使用 `javax.*`，与 Spring Boot 3.x (Jakarta) 不兼容 |

详细技术选型见 [技术选型与决策记录（ADR）](../2.%20设计阶段/08-技术选型与决策记录（ADR）.md) ADR-001。

### 2. 工程结构

```
minimall
├── src/main/java
│   └── com.yirancrazy.minimall
│       ├── annotation/      # 自定义注解（Manager 等）
│       ├── api/             # 对外 Feign 接口、DTO
│       ├── config/          # 配置类
│       ├── constant/        # 常量、枚举
│       ├── controller/      # Controller 层（V1 后缀）
│       ├── exception/       # 自定义异常、全局处理
│       ├── interceptor/     # 拦截器
│       ├── manager/         # 跨 Mapper 编排层（@Manager）
│       ├── mapper/          # MyBatis Mapper
│       ├── pojo/            # Result、Page 等
│       └── service/         # 业务服务
├── src/main/resources
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-test.yml
│   ├── application-prod.yml
│   ├── mapper/              # MyBatis XML
│   ├── static/
│   └── templates/
└── src/test/java
```

### 3. 启动类

```text
启动类必须放在包根目录 com.yirancrazy.minimall
禁止使用默认 spring-boot-starter-parent
必须设置 spring.application.name
```

### 4. 配置管理

#### 4.1 Profile 划分

| Profile | 用途 | 数据源 |
|---------|------|--------|
| dev | 本地开发 | 本地 DB / 共享 dev |
| test | 单元/集成测试 | H2 内存库 |
| staging | 预发 | 预发 DB |
| prod | 生产 | 生产 DB |

切换方式：
```
-Dspring.profiles.active=prod
```

#### 4.2 application.yml 规范

- 公共配置放 `application.yml`
- 环境差异放 `application-{profile}.yml`
- 敏感信息（密码、密钥）**禁止**写在 yml 中，使用 Nacos / Vault / 环境变量
- 关键配置加注释说明

#### 4.3 自定义配置类

```text
使用 @ConfigurationProperties(prefix = "xxx") 绑定配置
配置类必须显式注册到 META-INF/spring/...AutoConfiguration.imports
```

### 5. Controller 规范

- 类名：`XxxControllerV1`，必须带版本后缀
- 路径：`/api/v1/xxx`
- 注入：构造注入（`@RequiredArgsConstructor`），**禁止**字段注入
- 入参：`@RequestBody` + `@Valid` + DTO
- 出参：`Result<T>` 包装，**禁止**直接返 VO
- Swagger/Knife4j 注解必须：`@Tag`、`@Operation`、`@Parameter`
- 禁用 `@RequestParam` Map 接收参数

### 6. Service 规范

- 接口与实现分离：`IUserService` + `UserServiceImpl`
- 事务：`@Transactional(rollbackFor = Exception.class)`，**禁止**默认 rollbackFor（仅 RuntimeException）
- 事务范围尽量小，**禁止**在事务内调用 RPC / MQ
- 读操作加 `@Transactional(readOnly = true)`
- Service **禁止**直接调用多个 Mapper，统一走 Manager 层
- 幂等接口必须含 `idempotencyKey`

### 7. Manager 规范

- 自定义注解 `@com.yirancrazy.minimall.annotation.Manager`
- 注解内含 `@Component` 或 `@Service`，注册为 Spring Bean
- 负责跨 Mapper 编排、第三方调用、事务边界
- **禁止** Controller 直接调用 Manager（必须经 Service）

### 8. 全局异常处理

```text
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(BizException.class)
  public Result<Void> handleBiz(BizException e) { ... }

  @ExceptionHandler(Exception.class)
  public Result<Void> handleUnknown(Exception e) {
    log.error("unknown error", e);
    return Result.fail("SYS_ERROR", "系统异常");
  }
}
```

- 业务异常：`BizException` → `Result.fail(错误码, 消息)`
- 未知异常：记录 ERROR 日志 + traceId，返回通用错误
- **禁止**在 Controller 内 try-catch 全部异常

### 9. 参数校验

- DTO 使用 JSR-303 注解：`@NotNull`、`@NotBlank`、`@Size`、`@Pattern`
- Controller 入参加 `@Valid`
- 校验失败由 `MethodArgumentNotValidException` 统一处理
- 业务规则校验放 Service 层

### 10. 拦截器与过滤器

- 鉴权、日志、跨域用拦截器 `HandlerInterceptor`
- 请求体包装、签名校验用过滤器 `Filter`
- 顺序：`Filter > Interceptor > AOP`
- 拦截器注册实现 `WebMvcConfigurer.addInterceptors`

### 11. 异步与线程池

- `@Async` 必须配置线程池，**禁止**用默认 `SimpleAsyncTaskExecutor`
- 线程池必须命名，便于排查
- 异步方法**禁止**在同一类内部调用（绕过代理）
- 异步结果用 `CompletableFuture` 链式编排

### 12. 缓存

- Spring Cache + Redis：`@Cacheable` / `@CachePut` / `@CacheEvict`
- key 命名：`模块:业务:ID`，如 `user:profile:1001`
- TTL 通过 `spring.cache.redis.time-to-live` 配置
- 缓存更新策略：Cache-Aside（读时填、写时删）
- **禁止**缓存大对象（> 1MB）

### 13. 定时任务

- `@Scheduled` 任务必须含分布式锁（避免多实例重复执行）
- 推荐使用 XXL-Job / Elastic-Job 统一管理
- 任务执行日志必须记录开始/结束/耗时
- 长任务必须支持中断与超时

### 14. 监控与健康检查

- 引入 `spring-boot-starter-actuator`
- 暴露端点：`/actuator/health`、`/actuator/info`、`/actuator/prometheus`
- 自定义健康检查实现 `HealthIndicator`
- K8s liveness/readiness 探针配置 `/actuator/health/liveness`、`/actuator/health/readiness`

### 15. 部署与 Profile

- 启动脚本统一（见 [部署运维文档模板](../5.%20部署阶段/02-部署运维文档模板.md)）
- 禁止使用 `java -jar` 直启，必须通过启动脚本
- JVM 参数显式指定，禁止使用默认值

### 16. 反模式（禁止）

- ❌ `@Autowired` 字段注入
- ❌ Controller 直接调 Mapper
- ❌ Service 内循环调 RPC
- ❌ 业务异常 `try-catch` 后吞掉
- ❌ 事务内调用 `@Async` 方法
- ❌ 缓存穿透场景无防护（需布隆过滤器 / 空值缓存）

## 相关文档链接

- 上游：[Java 编码规范](01-Java编码规范.md)、[系统架构设计文档](../2.%20设计阶段/01-系统架构设计文档.md)
- 横向：[MyBatis-Plus 规范](03-MyBatis-Plus规范.md)、[公共组件与工具使用说明](08-公共组件与工具使用说明.md)
- 下游：[代码评审标准](07-代码评审标准.md)

## 变更记录

| 日期 | 版本 | 变更人/角色 | 变更说明 |
|------|------|-------------|----------|
| 2026-07-27 | V1.0 | yirancrazy@gmail.com | 初稿创建，覆盖工程结构/Controller/Service/Manager/异常/缓存/异步 |
|      |      |             |          |
