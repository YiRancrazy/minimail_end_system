## 错误跟踪记录 - 2026-08-09\_01

### 错误信息

- **发现时间**: 2026-08-09 12:18
- **错误类型**: ✅ 副作用错误（端口占用）
- **错误等级**: □ 致命 ✅ 一般
- **相关修改记录**: 任务"前端应用全量测试启动"

### 错误现象

**错误表现**: minimall-gateway `spring-boot:run` 启动失败，进程退出码 1
**报错信息**:

```
APPLICATION FAILED TO START
Web server failed to start. Port 8080 was already in use.
```

**复现步骤**:

1. `cd D:\work\project\mini_mail_system`
2. `mvnw.cmd -pl minimall-gateway spring-boot:run`
3. 启动后立刻退出，无 Tomcat 端口

### 根本原因分析

**直接原因**: 本机 8080 端口被残留 Java 进程（PID 40552 等）占用
**根本原因**: dev profile 启动失败后未清理 Spring Boot Maven Plugin fork 出的子进程；本机长期运行多服务时容易堆积
**关联修改**:

- 修改记录ID: N/A
- 修改时间: 2026-08-09 12:18
- 不当操作: 没有先 scan 端口占用就启 Gateway

### 修复方案

**临时修复**:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | ForEach-Object {
  Stop-Process -Id $_.OwningProcess -Force
}
```

**最终修复**:
对所有业务服务（8201-8211、8080、5173-5175）启前先检查 + 用 `Start-Process` + bat 文件分离 stdout/stderr，避免长任务被 PowerShell 父子管道吞掉
**修复验证**:

```
GW PASS status=401 body={"code":"10001","message":"未登录或登录已过期","data":null,...}
FE user-web status=200 bytes=320
```

### 经验沉淀

**避免模式**:

1. `spring-boot:run` + `tee` 在 PowerShell 长任务下会被 reap，子进程随之死（已观察一次：'Started' 后立即退出）
2. 端口 8080/8201-8211 启动前必须先扫，因本机开发机残留多

**最佳实践**:

1. 启动 Spring Boot 用 `.bat` 文件 + `Start-Process -WindowStyle Hidden`；日志直接重定向到文件
2. 启前必扫 `Get-NetTCPConnection`；冲突 PID 用 `Stop-Process -Force`
3. 链路验证命令：`Invoke-WebRequest http://localhost:8080/api/v1/<svc>/<path>`，期望 401（说明 JWT 拦截正常）

**检查清单**:

- [ ] 8080 不被占用
- [ ] Gateway 启动后 `Started ... in N seconds` 出现
- [ ] curl 网关收到 401 不是 5xx
- [ ] 前端 5173/5174/5175 都返回 HTML
- [ ] 后端 8201-8211 都 listen

## 错误跟踪记录 - 2026-08-13_01

### 错误信息

- **发现时间**: 2026-08-13 16:48
- **错误类型**: ✅ 副作用错误（Flyway checksum 漂移）
- **错误等级**: □ 致命 ✅ 一般
- **相关修改记录**: 修复三个 P1 问题（Long 精度/购物车/地址字段）

### 错误现象

**错误表现**: `spring-boot:run` 启动失败，`FlywayValidateException: Migration checksum mismatch`，涉及 V32/V40/V41
**报错信息**:

```
Caused by: org.flywaydb.core.api.exception.FlywayValidateException: Validate failed: Migrations have failed validation
Migration checksum mismatch for migration version 32
-> Applied to database : -1041897894
-> Resolved locally    : -1699749612
```

**复现步骤**:

1. `./mvnw -pl minimall-user-service spring-boot:run`
2. 启动 10 秒后退出，报 checksum mismatch

### 根本原因分析

**直接原因**: DB 里 `flyway_schema_history.checksum` 与当前 `minimall-flyway-core` jar 内迁移文件校验和不一致
**根本原因**: `spring-boot:run` 单模块运行时从 `.m2` 加载旧版 flyway-core jar；重新 `install` flyway-core 后本地解析值变化，而 DB 仍存旧 checksum。V40/V41 曾被设为 -1 绕过校验，无法匹配新文件
**关联修改**:

- 修改记录ID: N/A
- 修改时间: 2026-08-13 16:55
- 不当操作: 手工 UPDATE checksum 后未与"当前 flyway-core 解析值"对齐，导致二次漂移

### 修复方案

**临时修复**: 直接用 SQL 对齐 checksum（MySQL，本机 root/dazhutizi，库前缀 `*_db`）：

```js
// node + mysql2，对所有库执行
UPDATE ${db}.flyway_schema_history SET checksum=? WHERE version=?;
// 当前解析值：V32=-1699749612 V40=1013410157 V41=1628085743
```

**最终修复**: 迁移文件归属 flyway-core 模块，任何 V*.sql 变更必须同步 `flyway repair`（或对齐 DB checksum）；修改迁移文件后优先全量 `install flyway-core` 再启动，避免 .m2 旧 jar 造成解析值漂移
**修复验证**: 四个服务（auth/user/cart/goods）均启动成功，端口 8201/8202/8204/8205 正常监听

### 经验沉淀

**避免模式**:

1. `spring-boot:run` 单模块启动依赖 `.m2` 快照；改 common/api/flyway-core 后必须先 `install`，否则运行时还是旧代码
2. 手工 UPDATE checksum 前先确认"当前 jar 的 Resolved 值"（日志有 `Applied to database` / `Resolved locally` 两行），别凭上一次报错填

**最佳实践**:

1. Flyway 迁移文件改动 → `./mvnw -pl minimall-flyway-core install` → 再启动业务服务
2. 校验失败先看日志 `Applied`/`Resolved` 两值，用脚本对全部 `*_db` 一次性对齐
3. 业务服务 jar 无主清单时（repackage 未执行），用 `spring-boot:run` 而非 `java -jar`

**检查清单**:

- [ ] 启动报错是 Flyway checksum 时，先查 Resolved 值再对齐 DB
- [ ] 改过 common/flyway-core 后记得 `mvn install`
- [ ] gateway 需连 Redis：`.env` 可能未生效，必要时显式 `$env:REDIS_HOST='localdev'`

