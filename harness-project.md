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

