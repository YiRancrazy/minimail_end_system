# Java 编码规范

## 文档信息

| 字段 | 内容 |
|------|------|
| 文档名称 | Java 编码规范 |
| 文档版本 | V1.0 |$
| 所属阶段 | 开发阶段 |
| 文档状态 | 已发布 |
| 创建人 | yirancrazy@gmail.com |
| 创建日期 | 2026-07-27 |
| 最后更新 | 2026-07-28 |[\$]

## 文档目的

> 统一 Java 编码风格与最佳实践，提升代码可读性、可维护性、可测试性，遵循《阿里巴巴 Java 开发手册（黄山版）》核心规约。

## 适用范围

- **适用对象**：所有 Java 后端开发
- **适用场景**：日常编码、Code Review、自动化检查
- **不适用范围**：纯前端、脚本工具

## 详细内容

### 1. 命名规范

| 元素 | 规则 | 示例 |
|------|------|------|
| 包名 | 全小写，点分隔 | `com.yirancrazy.app.user` |
| 类名 | 大驼峰，名词 | `UserService`、`OrderControllerV1` |
| 接口名 | 同类名，能力型可加 `able` | `IRepository`、`Payable` |
| 方法名 | 小驼峰，动词+名词 | `getUserById`、`placeOrder` |
| 变量名 | 小驼峰，禁用拼音与单字母（循环除外） | `userName`、`orderList` |
| 常量 | 全大写，下划线分隔 | `MAX_RETRY_COUNT` |
| 枚举 | 大驼峰，字段 `(code, alias, message)` | `OrderStatus.PAID` |
| 异常码 | 按服务分段，5位字符串 | 用户域10000-10999、商品域11000-11999 |
| BO/DO/DTO/VO | 后缀区分语义 | `UserBO`、`UserDTO`、`UserVO` |
| 异常类 | `XxxException` 结尾 | `BizException` |
| 工具类 | `XxxUtil` / `XxxUtils` + 私有构造 | `MoneyUtil` |

### 2. 注释规范

#### 2.1 类注释

```java
/**
 * @Author: 张三
 * @Description: 用户服务，提供用户注册、登录、信息查询等能力。
 * @Version: 1.0
 * @DateTime: 2026/7/28 16:21
 **/
public class UserService {
}
```

#### 2.2 方法注释（公开方法必须）

```java
/**
 * 根据用户ID查询用户信息。
 *
 * @param userId 用户ID，必须 > 0
 * @return 用户VO；若不存在返回 null
 * @throws BizException 当 userId 非法时
 */
public UserVO getById(Long userId) {
}
```

#### 2.3 块注释

- 关键算法、复杂业务逻辑必须注释
- 解释 **why** 而非 **what**
- 禁止 `// TODO` 长期遗留，及时关闭或转 Issue

### 3. 格式规范

- **缩进**：4 空格，禁止 Tab
- **行宽**：≤ 120 字符
- **括号**：K&R 风格（左括号不换行）
- **空行**：方法间 1 空行，类成员按逻辑分组
- **import**：禁止通配符 `.*`，按字母排序，去除无用
- **类成员顺序**：常量 → 静态变量 → 实例变量 → 构造 → 静态方法 → 实例方法 → 内部类

### 4. OOP 规范

| 原则 | 要求 |
|------|------|
| 单一职责 | 一个类一个理由变更 |
| 开闭原则 | 扩展开放、修改关闭 |
| 里氏替换 | 子类不破坏父类约定 |
| 接口隔离 | 接口最小化 |
| 依赖倒置 | 依赖抽象、不依赖具体 |

### 5. 集合与泛型

- 禁止 `List list = new ArrayList()`，必须带泛型
- 集合初始化尽量指定容量：`new ArrayList<>(16)`
- `Map` 遍历使用 `entrySet()`，避免重复查 key
- 禁止在 `foreach` 中增删元素，使用迭代器或 `removeIf`
- `Optional` 仅作返回值，不作参数/字段

### 6. 字符串与日期

- 字符串拼接使用 `StringBuilder` 或 `String.format`
- 大量拼接用 `StringBuilder.append`，**禁止**在循环内 `+=`
- 日志使用占位符 `log.info("user={}", userId)`，**禁止**字符串拼接
- 日期时间统一用 `java.time`（`LocalDateTime`、`Instant`），**禁止** `Date` / `Calendar`
- 时间戳统一 UTC 存储，DB 用 `DATETIME`，应用层按需转换时区

### 7. 异常处理

| 规则 | 说明 |
|------|------|
| 业务异常 | 抛 `BizException(错误码, 消息)`，由全局处理器转换 |
| 运行时异常 | 禁止捕获后吞掉，至少 `log.error` |
| 禁止 finally return | finally 中 return 会吞掉异常 |
| 禁止裸 Exception 捕获 | 捕获具体异常类 |
| 资源释放 | 使用 try-with-resources |
| 自定义异常 | 必须含错误码 + 消息，禁止只含 message |

### 8. 并发编程

- 线程池必须显式命名，便于排查
- 禁止 `Executors.newFixedThreadPool` 等无界队列
- 共享变量必须考虑可见性（`volatile` / 锁）
- `ConcurrentHashMap` 替代 `Hashtable` / `Collections.synchronizedMap`
- 锁粒度尽量小，锁对象用 `private final Object lock = new Object()`
- 高并发场景用 `LongAdder` 替代 `AtomicLong`
- 禁止在锁内调用 RPC / DB

### 9. 日志规范

- 框架：Logback（Log4j2 可选）
- 必须含 `traceId`（通过 MDC）
- 级别：`ERROR`（线上异常）、`WARN`（可恢复异常）、`INFO`（关键业务）、`DEBUG`（详细调试）
- **禁止**输出敏感信息：身份证、银行卡、密码、手机号（脱敏后输出）
- **禁止**字符串拼接日志
- 业务关键节点（订单创建、支付回调）必须 INFO

### 10. 性能与安全

- 金额用 `BigDecimal`，**禁止** `float` / `double`
- 序列化优先 `Jackson` / `FastJSON2`
- SQL 必须用参数绑定，**禁止**字符串拼接
- 用户输入必须校验（长度、格式、范围）
- 加密用 `BCrypt` 存密码、`AES` 对称加密敏感字段
- 大文件 IO 用 NIO / `Files`

### 11. Lombok 使用

| 场景 | 推荐 | 备注 |
|------|------|------|
| POJO | `@Data` | 等价于 getter/setter/toString/equals/hashCode |
| 构造 | `@RequiredArgsConstructor` | 注入 final 字段 |
| 建造者 | `@Builder` | 参数 > 4 个时 |
| 日志 | `@Slf4j` | 替代手动声明 Logger |
| **禁止** | `@SneakyThrows` | 掩盖异常 |

### 12. 单元测试要求

- 公共方法覆盖率 ≥ 80%
- 关键业务 ≥ 90%
- 命名：`should_{预期}_when_{条件}`
- 独立性：禁止依赖执行顺序
- 详细见 [测试策略与计划](../4.%20测试阶段/01-测试策略文档.md)

### 13. 代码质量自动化配置

本项目使用全套代码质量自动化配置：Checkstyle（阿里规约模板）+ P3C（Alibaba Code Guidelines）+ ArchUnit（分层架构守卫）+ SonarQube（质量门禁），均在 CI 中集成执行。

### 14. 业务异常码规范

统一返回 `Result<T>`，成功码 `00000`，业务错误 `1xxxx`，系统错误 `2xxxx`。

异常码按服务分段（ADR-051）：

| 服务域 | 异常码范围 | 示例 |
|--------|------------|------|
| 用户域 | 10000-10999 | 10001 用户不存在 |
| 商品域 | 11000-11999 | 11001 商品已下架 |
| 订单域 | 12000-12999 | 12001 订单不存在 |
| 支付域 | 13000-13999 | 13001 支付超时 |
| 库存域 | 14000-14999 | 14001 库存不足 |
| 通知域 | 15000-15999 | 15001 短信发送失败 |
| 平台域 | 16000-16999 | 16001 权限不足 |

各服务在 `constant/` 包下定义枚举类，字段 `(code, alias, message)`。

### 15. 反模式（禁止）

- ❌ 字段注入（`@Autowired` 字段）→ 用构造注入
- ❌ 业务代码中 `System.out.println`
- ❌ 捕获异常后只 `e.printStackTrace()`
- ❌ 在 `for` 循环内创建 `SimpleDateFormat`
- ❌ `Map<String, Object>` 满天飞
- ❌ 静态变量持有可变状态
- ❌ 长方法（> 80 行）、长参数列表（> 5 个）

## 相关文档链接

- 上游：[系统架构设计文档](../2.%20设计阶段/01-系统架构设计文档.md)
- 横向：[SpringBoot 使用规范](02-SpringBoot使用规范.md)、[MyBatis-Plus 规范](03-MyBatis-Plus规范.md)
- 下游：[代码评审标准](07-代码评审标准.md)

## 变更记录

| 日期 | 版本 | 变更人/角色 | 变更说明 |
|------|------|-------------|----------|
| 2026-07-27 | V1.0 | yirancrazy@gmail.com | 初稿创建，涵盖命名/格式/OOP/异常/并发/日志/性能/安全 |
| 2026-07-28 | V1.1 | yirancrazy@gmail.com | 新增业务异常码规范章节 |
