# Handoff: RuoYi-Vue3 项目管理系统 — 环境搭建与技能配置完成

**日期**：2026-05-27  
**仓库**：`https://github.com/wy823011974-ux/pms-ruoyi` (4 commits, master 分支)  
**本地路径**：`D:\project-ruoyi`

---

## 背景

用户有一个已完成的 **FastAPI + Alpine.js 项目管理系统 v1.6-beta** (`D:\project`，93 commits)，功能完整——用户管理、项目管理、文件类型配置、字段定义、Excel上传校验、数据脱敏、仪表盘等。

用户决定用 **RuoYi-Vue3 架构**（Spring Boot + Vue 3 + Element Plus）重建该系统，实现功能对齐并升级优化。`D:\project-ruoyi` 是上一轮对话中创建的项目骨架，有 3 次提交、63 个文件，6 个业务模块已创建但**功能不完整**。

---

## 本次完成

### 1. 开发环境检查与安装
- ✅ Docker 29.4.3 / Docker Compose v5.1.4
- ✅ Node.js v24.16.0 / npm 11.13.0
- ✅ **JDK 17** — winget 安装 Microsoft OpenJDK 17.0.19（`C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot`）
- ✅ **Maven 3.9.9** — 手动下载后解压到 `C:\maven\apache-maven-3.9.9`，MAVEN_HOME 和 JAVA_HOME 已写入用户环境变量
- ✅ Git 2.54.0

### 2. Agent 技能体系配置
- `CLAUDE.md` — 项目规则 + `## Agent skills` 索引块
- `docs/agents/issue-tracker.md` — GitHub Issues（`gh` CLI）
- `docs/agents/triage-labels.md` — 中文五标签：`待评估`/`待补充`/`AI可处理`/`需人工`/`不处理`
- `docs/agents/domain.md` — 单上下文布局（`CONTEXT.md` + `docs/adr/`）

### 3. GitHub 仓库创建
`gh repo create pms-ruoyi --private` → `wy823011974-ux/pms-ruoyi`，已推送。

---

## 项目当前状态

### 已实现的模块（各行代码见提交 `18a5746`）

| 层级 | 模块 | 完成度 | 说明 |
|------|------|--------|------|
| 后端-基础设施 | Spring Security + JWT + BCrypt | 85% | 认证流程完整，权限检查粗粒度 |
| 后端-认证 | 登录/注册/me（手机号） | 80% | 登录锁定机制已有，审计日志未记 |
| 后端-项目 | CRUD + 状态流转 + 软删除 + 分页 | 75% | 缺少项目编码自动生成，creatorId写死 |
| 后端-调查数据 | Excel上传 + 校验 + 脱敏 + 分页查询 | 70% | 导出是TODO桩 |
| 后端-服务 | ExcelValidator + Anonymizer | 80% | 多Sheet、跳行、脱敏规则均已实现 |
| 后端-其他 | FileController, UserController, ProjectTypeController | ~50% | 骨架存在，需细读确认缺口 |
| 前端-框架 | Vue3 + Pinia + Router + Element Plus | 95% | 中文语言包、图标全量注册 |
| 前端-布局 | 侧边栏 + 顶栏 + 角色标签 | 85% | 修改密码弹窗无API对接 |
| 前端-页面 | 登录/仪表盘/项目列表 | 60% | 仪表盘只4个统计卡，项目表单字段少 |

### 关键缺口

1. **项目编码自动生成** — 旧系统有 `HBXM-{类型}-{年份}-{序号}` 规则
2. **项目类型配置页** — 旧系统有完整的文件类型 + 字段定义 + Schema导入导出
3. **用户管理页** — 路由已配置但页面功能未知
4. **数据导出 Excel** — SurveyController export 是 TODO
5. **审计日志** — 表已建但代码里没写
6. **权限精细化** — 路由 meta 有 roles 但后端未检查
7. **前端 survey/index.vue + file/index.vue + projectType/index.vue + user/index.vue** — 需细读评估

---

## 下一步计划

按优先级排列：

1. **启动项目** — `docker compose up -d`，验证 MySQL + Redis + 后端 + 前端四容器正常
2. **功能差距审计** — 细读 4 个未审查的前端页面 + FileController/UserController/ProjectTypeController，出完整差距清单
3. **补齐后端** — 项目编码生成、审计日志、数据导出、权限检查
4. **补齐前端** — 项目类型配置（文件类型+字段+Schema导入导出）、用户管理、调查数据面板
5. **测试** — API 测试 + E2E 测试

---

## 关键路径

```
D:\project-ruoyi\
├── CLAUDE.md                          ← 项目规则 + Agent技能索引
├── docker-compose.yml                 ← MySQL:3307 + Redis:6379 + backend:8080 + frontend:80
├── sql/01-init-schema.sql             ← 7张表 + 默认admin用户
├── backend/
│   ├── Dockerfile
│   ├── pom.xml                        ← Spring Boot 3.2.5 + MyBatis-Plus 3.5.6
│   └── src/main/java/com/pms/
│       ├── PMSApplication.java
│       ├── config/SecurityConfig.java
│       ├── common/{Result.java, GlobalExceptionHandler.java}
│       ├── framework/security/{JwtUtils.java, JwtAuthFilter.java}
│       ├── services/{ExcelValidator.java, Anonymizer.java}
│       └── modules/
│           ├── auth/AuthController.java
│           ├── dashboard/DashboardController.java
│           ├── project/{ProjectController.java, PmsProject.java}
│           ├── projectType/{ProjectTypeController.java, PmsProjectType.java}
│           ├── fileType/{PmsFileTypeConfig.java, PmsFieldDefinition.java}
│           ├── survey/{SurveyController.java, PmsSurveyData.java, PmsUploadHistory.java}
│           ├── file/{FileController.java, PmsFileRecord.java}
│           └── user/{UserController.java, SysUser.java}
├── frontend/
│   ├── Dockerfile + nginx.conf
│   ├── vite.config.js
│   └── src/
│       ├── main.js                    ← Element Plus + Pinia + Router
│       ├── router/index.js            ← 6 路由（login + 5 内页）
│       ├── stores/user.js             ← Pinia 用户状态
│       ├── api/{auth,project,survey,user,request}.js
│       ├── layout/index.vue           ← 侧边栏 + 顶栏
│       └── views/
│           ├── login/index.vue
│           ├── dashboard/index.vue
│           ├── project/index.vue
│           ├── projectType/index.vue
│           ├── survey/index.vue
│           ├── file/index.vue
│           └── user/index.vue
└── docs/agents/{issue-tracker,triage-labels,domain}.md
```

---

## 参考资源

- 旧系统技术文档：`D:\project\docs\SYSTEM-DOCUMENTATION.md`
- 旧系统需求规格：`D:\project\docs\superpowers\specs\2026-05-24-项目管理系统-需求设计文档.md`
- 旧系统交接文档：`D:\project\handoff-2026-05-26.md`
- 旧系统前端设计规范：`D:\project\docs\frontend-design-system.md`

---

## Suggested Skills

接手后先加载 `using-superpowers`，然后根据阶段使用：

- **启动验证**：直接 `cd D:\project-ruoyi && docker compose up -d --build`，检查 `http://localhost` 和 `http://localhost:8080/api/health`
- **功能差距审计**：阅读前端页面和后端控制器，对照旧系统需求规格出清单
- **后端开发**：`api-testing` 技能做接口测试
- **前端开发**：`frontend-e2e-testing` 做浏览器测试，`ui-ux-pro-max` 做设计决策
- **架构决策**：`grill-with-docs` 审阅设计，写 ADR
- **安全审计**：`security-review` 检查代码变更
