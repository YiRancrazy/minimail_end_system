# 构建与部署文档（CI/CD）

## 文档信息

| 字段 | 内容 |
|------|------|
| 文档名称 | 构建与部署文档（CI/CD） |
| 文档版本 | V1.0 |$
| 所属阶段 | 部署阶段 |
| 文档状态 | 已发布 |
| 创建人 | yirancrazy@gmail.com |
| 创建日期 | 2026-07-27 |
| 最后更新 | 2026-07-28 |[\$]

## 文档目的

> 定义从代码提交到生产上线的完整 CI/CD 流程，含构建、测试、镜像、灰度、回滚，保障发布可控、可追溯。

## 适用范围

- **适用对象**：研发、运维、SRE
- **适用场景**：日常发布、紧急回滚、灰度验证
- **不适用范围**：首次环境搭建（见 [项目初始化文档](03-项目初始化文档.md)）

## 详细内容

### 1. 流水线总览

```
代码提交 → CI（编译+测试+扫描+镜像）→ CD（部署到 dev/test/staging/prod）→ 监控
   │              │                            │
   │              │                            └─ 灰度发布
   │              └─ 镜像推送至 Harbor
   └─ 触发：PR / Merge main / Tag
```

### 2. 环境分级

| 环境 | 用途 | 部署触发 | 入口 |
|------|------|----------|------|
| dev | 开发自测 | Merge main | 自动 |
| test | 集成测试 | Merge main | 自动 |
| staging | 预发验证 | Tag v*.*.*-rc | 自动 |
| prod | 生产 | Tag v*.*.* | 手动审批 |

### 3. CI 阶段

#### 3.1 阶段 1：构建

```yaml
build:
  stage: build
  image: maven:3.8.6-eclipse-temurin-17
  script:
    - mvn -B clean package -DskipTests
  artifacts:
    paths: [target/*.jar]
    expire_in: 1 day
```

#### 3.2 阶段 2：单元测试

```yaml
test:
  stage: test
  image: maven:3.8.6-eclipse-temurin-17
  script:
    - mvn -B test
  coverage: '/Total.*?([0-9]{1,3})%/'
  artifacts:
    reports:
      junit: target/surefire-reports/*.xml
      coverage_report:
        coverage_format: jacoco
        path: target/site/jacoco/jacoco.xml
```

#### 3.3 阶段 3：静态扫描

```yaml
scan:
  stage: scan
  script:
    - mvn -B checkstyle:check
    - mvn -B com.github.spotbugs:spotbugs-maven-plugin:check
    - mvn -B org.owasp:dependency-check-maven:check
  allow_failure: false
```

#### 3.4 阶段 4：构建镜像

```yaml
docker:
  stage: docker
  image: docker:24
  services: [docker:24-dind]
  script:
    - docker build -t ${REGISTRY}/${PROJECT}:${CI_COMMIT_SHORT_SHA} .
    - docker push ${REGISTRY}/${PROJECT}:${CI_COMMIT_SHORT_SHA}
  only: [main, tags]
```

### 4. Dockerfile 模板

```dockerfile
# 多阶段构建
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
RUN groupadd -r app && useradd -r -g app appuser
WORKDIR /app
COPY --from=builder /build/target/minimall-*.jar /app/app.jar
RUN chown -R appuser:app /app
USER appuser
EXPOSE 8080
ENV JAVA_OPTS="-Xms1g -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT exec java $JAVA_OPTS -jar /app/app.jar
```

### 5. CD 阶段

#### 5.1 K8s 部署（推荐）

`deployment.yaml`：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: minimall
  namespace: ${env}
spec:
  replicas: 2
  selector:
    matchLabels: {app: minimall}
  template:
    metadata:
      labels: {app: minimall}
    spec:
      containers:
        - name: app
          image: ${REGISTRY}/${PROJECT}:${TAG}
          ports: [{containerPort: 8080}]
          envFrom:
            - configMapRef: {name: minimall-config}
            - secretRef:    {name: minimall-secret}
          resources:
            requests: {cpu: 500m, memory: 1Gi}
            limits:   {cpu: 2000m, memory: 2Gi}
          readinessProbe:
            httpGet: {path: /actuator/health/readiness, port: 8080}
            initialDelaySeconds: 30
          livenessProbe:
            httpGet: {path: /actuator/health/liveness, port: 8080}
            initialDelaySeconds: 60
```

#### 5.2 传统部署（无 K8s）

`deploy.sh`（伪代码）：

```bash
#!/bin/bash
set -e
APP_HOME=/app/minimall
ENV=$1
NEW_PKG=$2

# 1. 备份
mv ${APP_HOME}/current ${APP_HOME}/backup-$(date +%Y%m%d%H%M%S)

# 2. 解压新包
mkdir -p ${APP_HOME}/current
tar -xzf ${NEW_PKG} -C ${APP_HOME}/current

# 3. 加载配置
source /etc/profile
export SPRING_PROFILES_ACTIVE=${ENV}

# 4. 优雅停机
if [ -f ${APP_HOME}/app.pid ]; then
  kill -15 $(cat ${APP_HOME}/app.pid)
  sleep 10
fi

# 5. 启动
cd ${APP_HOME}/current
nohup java $JAVA_OPTS -jar app.jar > /app/logs/minimall/startup.log 2>&1 &
echo $! > ${APP_HOME}/app.pid

# 6. 健康检查
for i in {1..30}; do
  if curl -fs http://localhost:8080/actuator/health; then
    echo "started ok"
    exit 0
  fi
  sleep 2
done
echo "startup failed"
exit 1
```

### 6. 灰度发布

| 策略 | 适用 | 实现 |
|------|------|------|
| 蓝绿 | 大版本切换 | 流量整体切换 |
| 滚动 | 常规发布 | K8s `maxUnavailable=0, maxSurge=1` |
| 金丝雀 | 高风险功能 | 5% → 25% → 50% → 100% |
| A/B | 业务实验 | 路由规则 + 埋点 |

K8s 滚动更新：

```bash
kubectl set image deployment/minimall minimall=${REGISTRY}/${PROJECT}:${TAG} -n ${env}
kubectl rollout status deployment/minimall -n ${env}
```

### 7. 回滚

#### 7.1 自动回滚

- 启动 5 分钟内健康检查连续失败 3 次 → 触发回滚
- 错误率 > 阈值（默认 5%）持续 5 分钟 → 回滚

#### 7.2 手动回滚

K8s：

```bash
kubectl rollout undo deployment/minimall -n prod
```

传统：

```bash
# 1. 停服
kill -15 $(cat /app/minimall/app.pid)

# 2. 切回旧包
rm -rf /app/minimall/current
ln -s /app/minimall/backup-${PREVIOUS} /app/minimall/current

# 3. 启动
sh /app/minimall/bin/start.sh
```

详细流程见 [回滚方案](07-回滚方案.md)。

### 8. 发布清单（生产）

- [ ] PR 已合并到 main
- [ ] CI 全绿（编译/测试/扫描/镜像）
- [ ] 已在 staging 验证
- [ ] 数据库变更已执行
- [ ] 配置已更新（Nacos / ConfigMap）
- [ ] 通知相关方（业务、客服、运维）
- [ ] 值班人员已就位
- [ ] 监控大盘就绪
- [ ] 回滚预案已就绪
- [ ] 5 分钟观察期 + 1 小时重点监控

### 9. 监控与告警

发布期间重点观察：

- 启动时长（< 60s）
- 健康检查通过率
- 错误率（5xx 比例）
- 响应时间（P99）
- 慢 SQL 数
- JVM 堆使用

详细配置见 [监控告警文档](06-监控告警文档.md)、[日志与监控约定](05-日志与监控约定.md)。

### 10. 反模式（禁止）

- ❌ 跳过分阶段直接上生产
- ❌ 无审批直接发布
- ❌ 无回滚预案的发布
- ❌ 镜像未签名 / 漏洞扫描未通过
- ❌ 启动不健康即放过

## 相关文档链接

- 上游：[自动化测试规范](../4.%20测试阶段/04-自动化测试规范.md)
- 横向：[部署运维文档模板](02-部署运维文档模板.md)、[回滚方案](07-回滚方案.md)
- 下游：[监控告警文档](06-监控告警文档.md)、[故障排查手册](08-故障排查手册.md)

## 变更记录

| 日期 | 版本 | 变更人/角色 | 变更说明 |
|------|------|-------------|----------|
| 2026-07-27 | V1.0 | yirancrazy@gmail.com | 初稿创建，CI/CD 完整流程 |
|      |      |             |          |
