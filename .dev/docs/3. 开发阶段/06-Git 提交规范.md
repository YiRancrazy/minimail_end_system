# Git 提交规范与分支管理策略

## 文档信息

| 字段 | 内容 |
|------|------|
| 文档名称 | Git 提交规范与分支管理策略 |
| 文档版本 | V2.0 |
| 所属阶段 | 开发阶段 |
| 文档状态 | 已发布 |
| 创建人 | yirancrazy@gmail.com |
| 创建日期 | 2026-07-27 |
| 最后更新 | 2026-07-29 |

## 文档目的

> 统一 Git 提交信息格式与分支管理策略，保证提交历史清晰、可追溯、可自动化。
> 本文件是仓库 Git 提交和分支命名的唯一规范源；其他文档只引用本文件，不重复定义另一套规则。

## 适用范围

- **适用对象**：所有研发与 DevOps 人员。
- **适用场景**：日常提交、PR、CI 校验、Changelog 生成。
- **不适用范围**：仓库托管平台的权限与流水线细节。

> 本规范结合 Conventional Commits 1.0.0 制定。一次提交只做一个原子变更，分支短而具体，禁止长期悬挂。

---

## 1. 提交消息结构

```text
<type>(<scope>): <subject>

<body>

<footer>
```

- `type`：必填，使用英文固定类型。
- `scope`：建议填写，使用英文小写 kebab-case。
- `subject`：必填，使用中文动词开头的动作描述，≤72 字符，不加句号。
- `body`：可选；复杂变更必须说明动机、实现要点和影响范围。
- `footer`：可选；用于关联需求、缺陷或不兼容变更。

## 2. 提交类型（type）

| type | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | 新增接口或业务能力 |
| `fix` | 缺陷修复 | 修复异常、错误映射或边界问题 |
| `docs` | 文档变更 | 仅修改 Markdown、README 或注释文档 |
| `style` | 格式调整 | 不改变逻辑的格式化、换行或 import 调整 |
| `refactor` | 重构 | 不新增功能、不修复缺陷的结构调整 |
| `perf` | 性能优化 | 缓存、分页或查询优化 |
| `test` | 测试变更 | 新增或调整测试 |
| `build` | 构建或依赖 | Maven、插件或外部依赖调整 |
| `ci` | 流水线变更 | CI/CD 配置调整 |
| `chore` | 其他工程维护 | gitignore、EOL、脚本或工具维护 |
| `revert` | 回滚 | 回滚已有提交 |

## 3. 提交范围（scope）

`scope` 使用英文小写 kebab-case，优先使用实际模块名或稳定技术域名。

**业务模块**：`auth`、`user`、`merchant`、`goods`、`cart`、`order`、`pay`、`stock`、`notify`、`platform`、`id`。

**技术模块**：`common`、`api`、`gateway`、`flyway`、`pom`、`ci`、`eol`、`gitignore`、`ai-guidelines`。

规则：

- 禁止中文、拼音、作者名、时间戳和临时标识。
- scope 应简短、稳定、可检索；不使用随意的 `misc`、`tmp` 或 `test1`。
- 一次提交跨多个范围时优先拆分；确实无法拆分时使用逗号：`feat(order,pay): 新增支付后订单推进`。
- 无法合理归类时允许省略 scope，不得为了填充而使用模糊 scope。

## 4. 主题行（subject）

- 使用中文，描述做了什么，而不是“修改了什么”。
- 以 `新增`、`修复`、`调整`、`重构`、`移除`、`升级`、`回滚` 等动词开头。
- 不加句号，长度不超过 72 字符。
- 类名、模块名、命令和其他专有名词可保留英文原文。

正例：

```text
feat(order): 新增订单取消接口
fix(pay): 修复支付回调验签失败
docs(ai-guidelines): 更新协作约定
```

反例：

```text
feat: update code
fix: bug fixed
feat(订单): 新增取消接口
```

## 5. 正文（body）

简单且单一的变更可以省略 body。复杂变更必须包含：

- 变更动机：为什么要改。
- 实现要点：关键设计、逻辑或配置。
- 影响范围：涉及的模块、类、接口或部署影响。

正文可使用简短散文或 bullet 列表；每行建议不超过 72 字符，不重复 subject。

```text
docs(ai-guidelines): 更新提交规范

- 统一 type 使用英文 Conventional Commits 类型
- 统一 scope 使用英文小写 kebab-case
- 明确 body、footer 和分支命名规则
```

## 6. 脚注（footer）

- 关联需求或缺陷：`Closes #12`、`Refs: TAPD-1001`。
- 不兼容变更必须使用 `BREAKING CHANGE:`，说明冲突和迁移方式。
- 也可在 type 后使用 `!`：`feat(api)!: 统一 Result.code 语义`。

```text
feat(api)!: 统一 Result.code 语义

BREAKING CHANGE: 错误码改为五位字符串，客户端需要更新解析逻辑。
```

## 7. 提交禁用内容

以下内容不得出现在提交标题或正文中：

- 任何带 `iter` 的迭代标记。
- `.dev/docs/...` 路径、其中的文件名，以及反向引用外部规范位置的文案。
- `§`、`¶`、`Chapter`、`Section`、`Article`、`Appendix`、`Sec.`。
- “第 N 章、第一节、第 N 条、第一项”等章节序号。
- 密码、Token、密钥、内部地址等敏感信息。

## 8. 分支管理策略

仓库采用主题分支模型，`main` 是受保护的发布分支，只通过 PR 合入。

### 8.1 分支命名

格式：

```text
<type>/<scope>-<short-desc>
```

示例：

```text
feat/order-add-cancel-api
fix/pay-callback-signature
refactor/common-event-bus
hotfix/pay-callback-timeout
release/v1.2.0
```

规则：

- type 使用提交规范中的英文类型。
- scope 使用英文小写 kebab-case。
- short-desc 使用英文小写 kebab-case，保留必要的功能编号但不得使用全大写。
- 禁止作者名、时间戳和临时标识。
- 已合并分支及时删除；远程分支使用 `git push origin --delete <branch>` 清理。
- 工作分支原则上不超过 7 天；过期分支应重开或说明原因。

### 8.2 长期分支与紧急修复

- `main`：发布分支，禁止直接推送和对共享分支强制推送。
- `hotfix/<scope>-<short-desc>`：从 `main` 创建，修复后合入并回灌仍在维护的目标分支。
- `release/<version>`：发版预热分支，完成验证后合入 `main` 并创建版本 tag。

### 8.3 工作流

```bash
# 从最新 main 创建主题分支
git checkout main && git pull
git checkout -b feat/order-add-cancel-api

# 长期分支同步 main
git fetch origin && git rebase origin/main
```

合并方式按 PR 情况选择：

- **Squash and merge**：常规功能或提交较多、需要压缩历史时使用。
- **Rebase and merge**：提交原子且清晰、需要保留提交粒度时使用。
- **Merge commit**：长期分支、复杂协作或需要保留分支拓扑时使用。

合并后必须删除远程主题分支，main 保持可发布状态。

## 9. AI 协作与提交前检查

- 动手前检查 `git status` 和 `git branch --show-current`，确认不在 `main` 上直接修改。
- 提交前复核 `git status --short`、`git diff --stat` 和完整 diff。
- 改动跨多个 scope 时优先拆分提交。
- 未经用户明确要求，不创建提交、不推送、不提 PR。

## 10. 自动化校验

- 使用 `commitlint` 与 `@commitlint/config-conventional` 校验格式。
- 使用 `commit-msg` Hook 检查提交标题。
- CI 至少校验 type 枚举、subject 长度和提交格式。

示例：

```javascript
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [2, 'always', ['feat', 'fix', 'docs', 'style', 'refactor', 'perf', 'test', 'build', 'ci', 'chore', 'revert']],
    'subject-max-length': [2, 'always', 72],
    'subject-case': [0]
  }
};
```

## 相关文档链接

- 横向：[代码评审标准](07-代码评审标准.md)
- 下游：[构建与部署文档（CI/CD）](../5.%20部署阶段/01-构建与部署文档（CI-CD）.md)
- 参考：Conventional Commits 1.0.0

## 变更记录

| 日期 | 版本 | 变更人/角色 | 变更说明 |
|------|------|-------------|----------|
| 2026-07-29 | V2.0 | AI 协作 | 统一提交类型、范围、分支与合并策略 |
| 2026-07-27 | V1.0 | yirancrazy@gmail.com | 初稿创建 |
