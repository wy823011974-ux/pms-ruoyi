# 项目管理系统 — 完整技术文档

> **版本**: v1.6-beta  
> **最后更新**: 2026-05-26  
> **93 次提交** | **11 个功能模块** | **~5000 行核心代码**

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈](#2-技术栈)
3. [系统架构](#3-系统架构)
4. [项目结构](#4-项目结构)
5. [数据库设计](#5-数据库设计)
6. [功能模块详解](#6-功能模块详解)
7. [API 接口文档](#7-api-接口文档)
8. [业务流程](#8-业务流程)
9. [安全设计](#9-安全设计)
10. [开发历程](#10-开发历程)
11. [部署指南](#11-部署指南)

---

## 1. 项目概述

### 1.1 项目背景

**海南海拔市场调查集团** 项目管理系统。用于管理市场调查项目全流程——从项目创建、数据上传、校验脱敏到数据导出分析。

### 1.2 核心能力

| 维度 | 能力 |
|------|------|
| 用户管理 | 5角色体系(超级管理员/管理员/负责人/成员/客户)、手机号登录、注册审批 |
| 项目管理 | 创建/编辑/删除、状态流转(进行中→已完结→已归档)、按年份地区组织 |
| 文件管理 | 上传/覆盖/版本管理/删除、多Sheet Excel支持 |
| 数据管理 | Excel校验(表头+类型+必填)、脱敏入库(姓名/身份证/手机/地址)、分页查询、导出Excel |
| 配置管理 | 项目类型配置、文件类型配置、字段定义、Schema导入导出 |

### 1.3 业务类型配置

| 项目类型 | 文件类型 | 字段数 | Sheet |
|----------|----------|--------|-------|
| 体育场地常态化普查 | 场地台账(汇总) | 72 | 场地 |
| | 场地台账(分项) | 15 | 分项场地 |
| | 场馆代码 | 0 | - |
| 体育锻炼达标测验 | 单项成绩表 | 15 | 成绩表(skip=1) |
| | 总成绩表 | 6 | 成绩表(skip=1) |
| 国民体质监测 | 常态化幼儿组 | 103 | 国民体质幼儿组 |
| | 常态化成人组 | 137 | 国民体质成人组 |
| | 常态化老年组 | 116 | 国民体质老年组 |
| | 非常态化幼儿组 | 22 | 幼儿组 |
| | 非常态化成人组 | 29 | 成人组 |
| | 非常态化老年组 | 25 | 老年组 |

---

## 2. 技术栈

### 2.1 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Python | 3.11+ | 开发语言 |
| FastAPI | 0.115.0 | Web 框架(异步/自动文档/类型校验) |
| Uvicorn | 0.30.6 | ASGI 服务器 |
| SQLAlchemy | 2.0.35 | ORM(对象关系映射) |
| Alembic | 1.13.2 | 数据库迁移管理 |
| PostgreSQL | 15→18 | 关系数据库(JSONB/枚举/外键) |
| Pydantic | 2.8.2 | 数据校验和序列化 |
| python-jose | 3.3.0 | JWT 令牌生成和验证 |
| passlib + bcrypt | 1.7.4 / 4.2.1 | 密码哈希 |
| pandas | 2.2.2 | Excel 文件读取和解析 |
| openpyxl | 3.1.5 | Excel 文件写入 |
| pypinyin | 0.51.0 | 中文转拼音(自动生成编码,如"场地台账"→"chang_di_tai_zhang" |

### 2.2 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| HTML5 + CSS3 | - | 页面结构和样式 |
| Tailwind CSS | CDN | 原子化 CSS 框架 |
| Alpine.js | 3.14.0 | 响应式前端框架(轻量替代 Vue/React) |
| Vanilla JavaScript | ES6+ | 前端逻辑(无构建工具依赖) |

### 2.3 基础设施

| 技术 | 用途 |
|------|------|
| Docker Compose | (已弃用→本地部署) 容器编排 |
| Nginx | (已弃用→FastAPI直连) 反向代理+静态文件 |
| Playwright | E2E 浏览器自动化测试 |
| Git + GitHub | 版本控制和代码托管 |

---

## 3. 系统架构

### 3.1 当前架构（本地部署）

```
浏览器 :8000
  └─ Uvicorn (FastAPI)
       ├─ /api/* → 后端路由(认证/项目/数据/用户...)
       ├─ /*     → 前端静态文件(SPA)
       └─ PostgreSQL :5433 (本机)
```

### 3.2 后端分层架构

```
backend/app/
├── main.py              # FastAPI 应用入口 + 路由注册
├── config.py            # 环境变量配置(Pydantic Settings)
├── database.py          # SQLAlchemy 引擎 + 会话工厂
├── middleware.py         # 速率限制(IP 120次/分) + 生产错误收敛
├── models/              # 数据模型层(SQLAlchemy ORM)
│   ├── user.py          #   用户(UserRole枚举)
│   ├── project.py       #   项目(ProjectStatus枚举)
│   ├── project_type.py  #   项目类型
│   ├── file_type_config.py # 文件类型配置
│   ├── field_definition.py # 字段定义
│   ├── file_record.py   #   文件记录
│   ├── survey_data.py   #   调查数据(含元数据列)
│   ├── upload_history.py #  上传历史
│   ├── audit_log.py     #   审计日志
│   └── system_log.py    #   系统日志
├── schemas/             # Pydantic 请求/响应模型
│   ├── user.py          #   用户Schema(注册/登录/管理)
│   ├── project_type.py  #   项目类型+文件类型+字段Schema
│   └── survey_data.py   #   调查数据Schema
├── routers/             # API 路由(控制器层)
│   ├── auth.py          #   登录/注册/改密码(JWT)
│   ├── users.py         #   用户CRUD(管理员专用)
│   ├── projects.py      #   项目CRUD
│   ├── dashboard.py     #   仪表盘统计
│   ├── project_types.py #   项目类型CRUD
│   ├── file_types.py    #   文件类型+字段CRUD+Schema导入导出
│   ├── files.py         #   文件上传/列表/版本/删除
│   └── survey_data.py   #   调查数据上传/列表/删除/导出/清空
├── services/            # 业务逻辑(纯函数)
│   ├── security.py      #   密码哈希+JWT令牌
│   ├── excel_validator.py # Excel校验引擎(表头+类型)
│   ├── anonymizer.py    #   数据脱敏引擎(M5)
│   └── super_admin.py   #   超级管理员配置同步
```

### 3.3 前端架构（SPA 单页应用）

```
frontend/
├── index.html           # 单页应用(所有页面/弹窗/面板/抽屉)
├── js/
│   ├── app.js           # Alpine.js 主控(状态+方法, 778行)
│   ├── api.js           # API 调用封装(10行,压缩格式)
│   ├── utils.js         # 工具函数(9行:Toast/日期/转义)
│   └── router.js        # Hash路由(37行:SAP导航)
└── css/
    └── style.css        # 自定义样式
```

**z-index 层级**: 面板(40) < 配置抽屉(50) < 字段抽屉(60) < 模态框(70) < Toast(9999)

**路由**: Hash-based SPA (`#/dashboard`, `#/projects`, `#/users`, `#/project-types`), `window.onhashchange` 监听, `navigateTo(page)` 统一入口

**页面模式**:
- **登录页**: 登录/注册切换表单(showRegister切换)
- **仪表盘**: 统计卡片(项目数/文件数/数据类型)
- **项目管理**: 卡牌列表(搜索/筛选/状态操作)+新建弹窗
- **用户管理**: 卡牌列表(角色切换/编辑/禁用/删除)+新建弹窗
- **项目类型配置**: 卡牌列表→右侧抽屉(文件类型表格+字段子抽屉)
- **文件管理面板**: 模态框(上传/版本历史/删除)
- **数据管理面板**: 模态框(上传/分页表格/导出/清空)

---

## 4. 项目结构

```
D:\project/
├── CLAUDE.md                 # AI 助手配置(角色+行为准则+工作流)
├── CHANGELOG.md              # 变更日志(v1.0→v1.6)
├── CONTEXT-MAP.md            # 多上下文导航
├── .gitignore                # Git忽略规则
├── start.bat                 # 开发环境一键启动
├── start_prod.bat            # 生产环境启动(含Nginx备忘)
├── restore_db.bat            # 数据库恢复工具
│
├── backend/                  # ├─ 后端 Python 项目
│   ├── .env                  #    环境变量(保密,不入Git)
│   ├── .env.example          #    环境变量模板(开发)
│   ├── .env.production.example #  环境变量模板(生产)
│   ├── requirements.txt      #    Python依赖清单(17个包)
│   ├── Dockerfile            #    Docker镜像定义
│   ├── alembic.ini           #    数据库迁移配置
│   ├── super_admins.json     #    超级管理员配置(仅此文件管理)
│   ├── alembic/versions/     #    11个迁移文件+1个.gitkeep
│   ├── app/                  #    应用源码
│   └── uploads/              #    上传文件存储
│
├── frontend/                 # ├─ 前端静态文件
│   ├── index.html            #    单页应用(468行)
│   ├── js/                   #    JavaScript模块
│   └── css/                  #    样式文件
│
├── seed/                     # ├─ 种子数据
│   ├── seed.py               #    主种子脚本(idempotent)
│   ├── import_missing.py     #    兜底导入脚本
│   ├── cleanup_test_data.py  #    测试数据清理
│   ├── export_schemas.py     #    Schema导出工具
│   ├── test_m5.py            #    M5脱敏E2E测试
│   ├── update_schema_from_2024.py # Schema从文件更新
│   ├── check_roles.py        #    角色检查工具
│   └── schemas/              #    10个Schema JSON文件
│
├── file_type_json/           # ├─ 导出的Schema配置
│   └── 11个JSON文件          #    对应11个文件类型
│
├── docs/                     # ├─ 文档
│   ├── adr/                  #    架构决策记录(3个)
│   ├── agents/               #    AI Agent配置
│   ├── decisions/            #    技术选型决策
│   ├── solutions/            #    问题知识卡片
│   ├── steps/                #    步骤完成文档
│   ├── superpowers/          #    AI规划设计文档
│   ├── devlog-*.md           #    开发日志
│   └── frontend-design-system.md # 前端设计规范(颜色/布局/按钮/层级)
│   └── skills-catalog.md      #   可用的AI Skills目录
│
├── tests/                    # ├─ 测试
│   ├── test_e2e_phase3.py    #    E2E浏览器测试(19项)
│   └── e2e_reports/          #    测试报告和截图
│
├── docker-compose.yml        # Docker编排(备用)
└── nginx.conf               # Nginx配置(备用)
```

---

## 5. 数据库设计

### 5.1 ER 关系图

```
users ──1:N──> projects ──1:N──> survey_data
  │               │                  │
  │               │                  ├── project_code (冗余)
  │               │                  ├── project_year (冗余)
  │               │                  ├── project_location (冗余)
  │               │                  ├── project_type_name (冗余)
  │               │                  ├── file_type_name (冗余)
  │               │                  └── file_type_config_id (FK)
  │               │
  │               └── project_type_id (FK)
  │
  └── project_types ──1:N──> file_type_configs ──1:N──> field_definitions
                                  │
                                  ├── has_fields (bool)
                                  ├── upload_schema (JSONB)
                                  ├── sheet_name (varchar)
                                  └── skip_rows (int)
```

### 5.2 核心表

#### users — 用户表(5角色)

| 列 | 类型 | 说明 |
|----|------|------|
| id | serial PK | |
| username | varchar(50) UNIQUE | 手机号=用户名 |
| email | varchar(100) UNIQUE | |
| hashed_password | varchar(255) | bcrypt哈希 |
| display_name | varchar(50) | 真实姓名 |
| phone | varchar(20) | 手机号(登录凭证) |
| department | varchar(100) | 所属部门/单位 |
| role | userrole enum | super_admin/admin/manager/member/customer |
| is_active | bool | 启用/禁用(注册需审批) |
| failed_login_count | int | 失败计数(锁定用) |
| locked_until | timestamptz | 锁定到期时间 |

#### projects — 项目表

| 列 | 类型 | 说明 |
|----|------|------|
| id | serial PK | |
| code | varchar(50) UNIQUE | 项目编号(HBXM-TYCD-2026-001) |
| name | varchar(200) | 项目名称 |
| year | int | 调查年份 |
| location | varchar(200) | 调查地区 |
| project_type_id | int FK | 关联项目类型 |
| status | projectstatus enum | 进行中/已完结/已归档 |
| creator_id | int FK | 创建人 |
| leader_id | int FK | 负责人 |
| dynamic_fields | JSONB | 动态字段值 |
| status | projectstatus enum | 进行中/已完结/已归档 |
| start_date | datetime | 开始日期 |
| end_date | datetime | 结束日期 |
| is_deleted | bool | 软删除标记 |
| created_at | timestamptz | 创建时间 |

#### file_records — 文件记录表

| 列 | 类型 | 说明 |
|----|------|------|
| id | serial PK | |
| project_id | int FK | 所属项目 |
| file_type_config_id | int FK | 文件类型配置 |
| filename | varchar(500) | 存储文件名 |
| original_name | varchar(500) | 原始文件名 |
| file_type | varchar(50) | 文件类型code |
| version | int | 版本号(default 1) |
| file_size | bigint | 字节数 |
| is_overwrite | bool | 是否覆盖 |
| uploaded_by | int FK | 上传人 |
| created_at | timestamptz | 上传时间 |

#### survey_data — 调查数据表(核心)

| 列 | 类型 | 说明 |
|----|------|------|
| id | bigserial PK | |
| project_id | int FK | 所属项目 |
| row_data | JSONB | 脱敏后的数据行 |
| file_type_config_id | int FK | 文件类型配置 |
| project_code | varchar(50) | 项目编号(冗余) |
| project_name | varchar(200) | 项目名称(冗余) |
| project_year | int | 年份(冗余) |
| project_location | varchar(200) | 地区(冗余) |
| project_type_name | varchar(100) | 项目类型(冗余) |
| file_type_name | varchar(100) | 文件类型(冗余) |
| original_filename | varchar(500) | 原始文件名 |
| uploaded_by | int FK | 上传人 |
| created_at | timestamptz | 上传时间 |

> **设计决策**: 元数据列冗余存储，便于BI工具直接`SELECT * FROM survey_data`按年份/地区/类型分类，无需JOIN。

### 5.3 数据库迁移链

```
893015bbf70f (初始: lock+soft_delete)
  → phase3_m6 (project_type_id+extra_attrs+skip_rows)
  → phase3_m7 (file_type_configs表)
  → phase3_m8 (FTC唯一约束)
  → phase3_m9 (字段→file_type迁移+has_fields+upload_schema)
  → phase3_m10 (sheet_name支持多Sheet)
  → phase3_m11 (用户扩展字段+新角色枚举)
  → phase3_m12 (角色枚举大小写对齐)
  → phase3_m13 (survey_data元数据列)
  → phase3_m14 (survey_data.file_type_config_id)
  → phase3_m15 (upload_history.file_type_config_id)
```

---

## 6. 功能模块详解

### 6.1 M1: 用户认证

**文件**: `backend/app/routers/auth.py:34-147`, `frontend/js/app.js:96-105`

#### 6.1.1 注册
- 端点: `POST /api/auth/register`
- 字段: phone(手机号=账号), email, password, display_name, department
- 手机号格式校验(11位1开头), 密码强度校验(8位+字母+数字)
- 注册后 is_active=False, 等待管理员审批
- 前端: 登录页"注册"链接→注册表单(5个必填字段)

#### 6.1.2 登录
- 端点: `POST /api/auth/login`
- 字段: account(手机号或用户名), password
- 支持手机号和用户名双模式(account字段)
- 失败计数: 累计5次锁定15分钟
- 成功: 重置失败计数和锁定时间
- JWT Token: HS256算法, 默认480分钟过期

#### 6.1.3 密码修改
- 端点: `PUT /api/auth/change-password`
- 需要验证旧密码
- bcrypt 哈希存储(60字符)

#### 6.1.4 登录锁定
- 5次失败→锁定15分钟
- 锁定期间返回423状态码+剩余时间
- 锁定过期后自动清除

**关键代码**:
```python
# auth.py:56-100
user = db.query(User).filter(
    (User.phone == account) | (User.username == account)
).first()

# 锁定检查
if user and user.locked_until:
    if user.locked_until > datetime.now(timezone.utc):
        remaining = int((user.locked_until - datetime.now(timezone.utc)).total_seconds() / 60) + 1
        raise HTTPException(status_code=423, detail=f"账号已锁定，请 {remaining} 分钟后再试")
```

---

### 6.2 M2: 项目管理

**文件**: `backend/app/routers/projects.py`, `frontend/js/app.js:137-241`

#### 6.2.1 项目CRUD
- 创建: 必填字段 project_type_id, name, code, year, location, leader_id, start_date, end_date
- 列表: 支持状态筛选、搜索, 只显示未删除项目
- 编辑: 修改基本信息
- 软删除: is_deleted=True, 仪表盘不计入统计

#### 6.2.2 状态流转
- 进行中 → 已完结 → 已归档
- 已完结可重开(→进行中)
- 已归档不可操作

#### 6.2.3 项目编号
- 格式: HBXM-{类型简写}-{年份}-{序号}
- 创建时自动生成

**前端**: 卡牌布局(max-w-4xl居中), 每张卡显示编号/名称/地区/状态/进度/操作按钮

---

### 6.3 M3: 文件管理

**文件**: `backend/app/routers/files.py`, `frontend/js/app.js:613-656`

#### 6.3.1 上传
- 端点: `POST /api/projects/{id}/upload`
- 支持格式: .xlsx/.xls/.docx/.pdf/.pptx/.txt/.csv
- 大小限制: 50MB
- 文件存储: `uploads/{project_code}/`
- 命名规则: , 如 `{project_code}-{file_type_code}-{YYYYMMDD}-v{version}{ext}`

#### 6.3.2 覆盖上传
- 默认覆盖模式
- 勾选"新增上传"保留旧版本
- 覆盖时删除旧物理文件, 复用版本号

#### 6.3.3 版本管理
- 每种文件类型保留版本链
- 列表只显示最新版本
- 可展开查看全部版本历史

#### 6.3.4 文件面板筛选
- 仅显示 `has_fields=false` 的文件类型(附件类)

---

### 6.4 M4: 数据上传与校验

**文件**: `backend/app/services/excel_validator.py`, `backend/app/routers/survey_data.py:66-201`

#### 6.4.1 上传流程

```
用户选文件 → 选文件类型 → 确定跳过行 → 上传
  → 扩展名校验(.xlsx/.xls)
  → MIME类型校验(支持None回退)
  → 文件头魔数校验(PK=ZIP / OLE2)
  → 大小校验(50MB)
  → pd.read_excel(sheet_name) 读取指定Sheet
  → 表头比对(期望vs实际,逐列diff)
  → 逐行逐列类型校验(text/number/date/select)
  → 脱敏处理(M5)
  → 入库(SurveyData,含元数据)
  → 记录上传历史(UploadHistory)
```

#### 6.4.2 校验引擎

**表头校验** (`_validate_headers`):
- 按期望字段列表匹配Excel表头行
- 支持 skip_rows 跳过前N行
- 不匹配时逐列报告差异(期望→实际,第N列)

**数据类型校验** (`_validate_data_rows`):
- text: 任意字符串
- number: 数字格式
- date: YYYY-MM-DD格式
- select: 选项列表匹配
- 逐行逐列收集所有错误

**错误报告**:
```
上传失败：表头字段不匹配
  第4列：期望「单位所在地址_省」→ 实际「省」
  第5列：期望「单位所在地址_市」→ 实际「市」
共 5 处差异
缺少的字段（5个）：单位所在地址_省、单位所在地址_市...
多余的字段（5个）：省、市...
```

#### 6.4.3 多Sheet支持
- FileTypeConfig.sheet_name 指定读取的Sheet
- None=第一个Sheet(默认)
- 不同Sheet对应不同文件类型配置

#### 6.4.4 跳过行控制
- FileTypeConfig.skip_rows: 默认跳过行数
- 前端可覆盖(输入框)
- 体育锻炼达标测验模板 skip_rows=1(跳过模板说明行)

#### 6.4.5 覆盖上传
- 默认覆盖(删除旧数据+导入新数据)
- 勾选"新增上传"保留旧数据
- 匹配条件: project_id + file_type_config_id

#### 6.4.6 已有数据警告
- 选择文件类型时检测是否已有数据
- 黄色提示: "当前数据类型已上传过数据，再次上传将覆盖之前的内容。若需新增请勾选「新增上传」"

---

### 6.5 M5: 数据脱敏

**文件**: `backend/app/services/anonymizer.py`

#### 6.5.1 脱敏时机
- 校验通过后、入库前实时脱敏
- 原始数据不入数据库
- 脱敏结果写入 SurveyData.row_data

#### 6.5.2 脱敏规则

| 规则 | 方法 | 输入 | 输出 |
|------|------|------|------|
| name | 保留姓,名替换* | 张三丰 | 张\*\* |
| name | 复姓保留前两字 | 欧阳锋 | 欧阳\* |
| name | 少数民族含· | 阿卜杜拉·买买提 | 阿卜杜拉·\*\*\* |
| id_card | 保留前6+后4 | 110101199003071234 | 110101\*\*\*\*\*\*\*\*1234 |
| phone | 保留前3+后4 | 13800138001 | 138\*\*\*\*8001 |
| address | 保留省市区 | 海南省海口市龙华区滨海大道 | 海南省海口市龙华区\*\*\*\* |

#### 6.5.3 脱敏字段配置
- 在字段定义中标注 `sensitive: true` + `anonymize_rule`
- 存储于 FieldDefinition.extra_attrs (JSONB)
- 前端字段编辑弹窗可配置

---

### 6.6 M6: 项目类型配置

**文件**: `backend/app/routers/project_types.py`, `backend/app/routers/file_types.py`

#### 6.6.1 项目类型管理
- CRUD: 名称/编码/描述
- 卡牌列表展示
- 点击"配置"→右侧抽屉

#### 6.6.2 文件类型配置
- 绑定在项目类型下
- 字段: 名称/编码(拼音自动生成(函数,去空格下划线连接))/跳过行/Sheet名称/需要字段校验/排序
- `has_fields`: 是否配置字段定义(决定出现在数据面板还是文件面板)
- `upload_schema`: JSONB, 字段定义的JSON表示

#### 6.6.3 字段定义
- 绑定在文件类型下
- 字段: 字段标识(key)/字段名称(label)/字段类型(text/number/date/select)/必填/启用/敏感
- extra_attrs: max_length/pattern/min/max/format/sensitive/anonymize_rule
- 二层抽屉: 文件类型→字段子抽屉

#### 6.6.4 Schema 导入导出
- 导出: 浏览器下载JSON文件
- 导入: JSON文本→覆盖字段定义
- 自动生成 upload_schema

---

#### 6.6.5 种子数据工具
- : 一键导入所有Schema(按code去重,idempotent)
- : 补导入缺失的FTC和字段
- : 从实际上传文件重新生成Schema
- : 删测试数据保留真实业务
- : 导出所有Schema到file_type_json/

---

## 6.7 M7: 用户管理

**文件**: `backend/app/routers/users.py`

#### 6.7.1 角色体系
| 角色 | 权限 |
|------|------|
| super_admin | 全部权限, 仅通过配置文件管理 |
| admin | 用户管理/项目类型配置/全部项目操作 |
| manager | 项目管理(负责人)/上传数据 |
| member | 查看项目/上传数据 |
| customer | 仅查看(预留) |

#### 6.7.2 安全约束
- 不能修改自己的角色
- 不能修改自己的手机号
- 不能禁用自己
- 不能删除自己
- super_admin不可编辑/禁用/删除/改密码
- 不能移除最后一个管理员

#### 6.7.3 超级管理员
- 唯一配置入口: `backend/super_admins.json`
- 后端启动时自动同步角色
- 前端:角色锁定显示,操作按钮隐藏

#### 6.7.4 编辑弹窗
- 姓名/电话/部门/角色/状态
- super_admin角色只读
- 自己手机号只读

---

### 6.8 仪表盘

**文件**: 

 返回: 

前端卡牌展示, 点击跳转到对应页面。

---

### 6.9 数据面板

**文件**: `frontend/js/app.js:662-720`, `frontend/index.html:378-465`

#### 6.8.1 数据展示
- 按文件类型筛选
- 分页(50条/页)
- 元数据列: 项目编号/年份/地区
- 动态列: 根据 row_data 的键

#### 6.8.2 导出Excel
- 端点: `GET /api/projects/{id}/survey-data/export`
- 包含: 元数据列 + 数据列
- 中文表头

#### 6.8.3 清空数据
- 端点: `DELETE /api/projects/{id}/survey-data/clear?file_type_config_id=`
- 确认弹窗
- 仅清空当前项目+所选文件类型

#### 6.8.4 上传历史
- 按文件类型筛选
- 折叠展开
- 显示状态/文件名/行数

---

## 7. API 接口文档

### 7.1 认证

| 方法 | 路径 | Request | Response | 说明 |
|------|------|---------|----------|------|
| POST | /api/auth/register | phone,email,password,display_name,department | message | 注册(is_active=false) |
| POST | /api/auth/login | account,password | access_token,user | 手机号或用户名登录 |
| GET | /api/auth/me | - | id,username,role... | 当前用户信息 |
| PUT | /api/auth/change-password | old_password,new_password | message | 修改密码 |

### 7.2 用户管理

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | /api/users/ | admin+ | 用户列表 |
| POST | /api/users/ | admin+ | 创建用户 |
| PUT | /api/users/{id} | admin+ | 编辑用户 |
| PUT | /api/users/{id}/reset-password | admin+ | 重置密码 |
| DELETE | /api/users/{id} | admin+ | 删除用户 |

### 7.3 项目管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/projects/ | 项目列表 |
| POST | /api/projects/ | 创建项目 |
| PUT | /api/projects/{id} | 编辑项目 |
| DELETE | /api/projects/{id} | 软删除 |
| PUT | /api/projects/{id}/status | 修改状态 |
| POST | /api/projects/{id}/restore | 恢复 |

### 7.4 文件管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/projects/{id}/upload | 上传文件(覆盖/新增) |
| GET | /api/projects/{id}/files | 文件列表 |
| GET | /api/files/{id}/versions | 版本历史 |
| DELETE | /api/files/{id} | 删除所有版本 |

### 7.5 调查数据

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/projects/{id}/upload-data | 上传数据(覆盖/新增/跳过行) |
| GET | /api/projects/{id}/survey-data | 数据列表(分页+FTC筛选) |
| GET | /api/projects/{id}/survey-data/export | 导出Excel |
| DELETE | /api/projects/{id}/survey-data/{data_id} | 删除单条 |
| DELETE | /api/projects/{id}/survey-data/clear | 清空类型数据 |
| GET | /api/projects/{id}/upload-history | 上传历史(FTC筛选) |

### 7.6 配置管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST | /api/project-types/ | 项目类型列表/创建 |
| PUT/DELETE | /api/project-types/{id} | 项目类型编辑/删除 |
| GET/POST | /api/project-types/{id}/file-types | 文件类型列表/创建 |
| PUT/DELETE | /api/project-types/{id}/file-types/{cid} | 文件类型编辑/删除 |
| GET/POST | /api/project-types/{id}/file-types/{cid}/fields | 字段列表/创建 |
| PUT/DELETE | /api/project-types/{id}/file-types/{cid}/fields/{fid} | 字段编辑/删除 |
| POST | /api/project-types/{id}/file-types/{cid}/schema/import | 导入Schema |
| GET | /api/project-types/{id}/file-types/{cid}/schema/export | 导出Schema |

### 7.7 其他

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/health | 健康检查 |
| GET | /api/dashboard/stats | 仪表盘统计 |
| GET | /api/docs | API文档(开发环境) |

---

## 8. 业务流程

### 8.1 数据上传完整流程

```
0. 系统启动: 同步super_admin角色, 创建上传目录, 运行数据库迁移
1. 管理员创建项目类型(ti_yu_chang_di_pu_cha)
2. 配置文件类型(场地台账汇总, has_fields=true, sheet_name=场地, skip_rows=0)
3. 配置字段定义(72个字段,部分标注sensitive+anonymize_rule)
4. 创建项目(2026年度体育场地普查, 关联上述类型)
5. 用户打开项目→数据面板→选择"场地台账汇总"
6. 选择Excel文件→跳过行0→上传
7. 系统: 扩展名校验(.xlsx)→MIME/魔数校验(PK签名)→pd.read_excel('场地')→表头逐列比对(含差异报告)→逐行逐列类型校验→收集所有错误
8. 系统: 读取sensitive字段→逐行脱敏→行数据附上项目元数据→入库
9. 用户: 查看分页数据→导出Excel→BI分析
```

### 8.2 数据查询流程



### 8.3 文件上传流程

```
1. 配置文件类型(分析报告, has_fields=false)
2. 用户打开项目→文件面板→选择"分析报告"
3. 选择PDF文件→上传
4. 系统: 扩展名校验→存储到uploads/{project_code}/→记录版本
5. 再次上传: 默认覆盖(version不变)→或新增(version+1)
```

---

## 9. 安全设计

| 层级 | 措施 |
|------|------|
| 认证 | JWT令牌(HS256,480分钟过期) |
| 密码 | bcrypt哈希(不可逆), 强度校验(8位+字母+数字) |
| 登录保护 | 5次失败锁定15分钟(423状态码) |
| SQL注入 | SQLAlchemy ORM参数化查询 |
| XSS | escapeHtml()过滤用户输入 |
| 文件上传 | 扩展名+MIME+魔数三重校验, 50MB限制 |
| 敏感数据 | 上传实时脱敏, 原始数据不入库 |
| 权限控制 | 5角色体系, 管理员API独立鉴权 |
| API限流 | 生产环境IP每分钟120次 |
| 错误收敛 | 生产环境隐藏内部异常 |
| 密钥管理 | .env不入Git, .env.example模板 |
| 超级管理员 | 仅通过super_admins.json配置, 前端锁定 |

---

## 10. 开发历程

### 10.1 提交时间线

| 阶段 | 提交数 | 主要成果 |
|------|--------|----------|
| 初始骨架 | 8 | 项目骨架+需求文档+Docker部署 |
| 第二阶段 | 10 | 软删除/状态流转/用户管理/仪表盘 |
| 第三阶段 | 15 | 项目类型配置/文件管理/字段定义/E2E测试 |
| 第四阶段 | 8 | M4数据上传校验/grill审阅修复 |
| 第五阶段 | 9 | 架构重构(字段归属/抽屉模式/UI统一) |
| 第六阶段 | 15 | 种子数据配置/多Sheet/Schema导出/测试清理 |
| 第七阶段 | 13 | 用户管理扩展/手机号登录/注册删除 |
| 第八阶段 | 8 | M5脱敏引擎/元数据列/数据面板增强 |
| 第九阶段 | 7 | 上传逻辑重构/去Docker化/安全加固/E2E修复 |

### 10.2 关键架构决策

1. **字段归属重构** (`1f80b2b`): FieldDefinition从ProjectType迁移到FileTypeConfig——一个项目类型可有多种文件类型，每种有自己的字段定义
2. **抽屉模式** (`72b76be`): 配置页从双面板改为卡片+右侧抽屉——解决小屏滚动问题
3. **手机号登录** (`5abda92`): username=手机号, account字段兼容旧账号
4. **多Sheet支持** (`2e1c16d`): sheet_name字段, 校验器支持按Sheet名读取
5. **脱敏引擎** (`e2e67c8`): 校验后入库前实时脱敏, 4条规则
6. **元数据冗余** (`709f556`): SurveyData附项目元数据, 便于BI直接查询
7. **去Docker化** (`0ff4987`): 从Docker Compose迁移到本机PostgreSQL+FastAPI直连
8. **has_fields分流** (`96ebb85`): 文件类型按has_fields分流到数据面板/文件面板

### 10.3 代码审查流程

所有功能模块均通过 **grill-with-docs** 审查流程:
1. 编写实现→2. grill审查(5-10个问题/轮)→3. 修复→4. 重审→5. 提交

关键审查轮次:
- M4数据校验: 两轮grill审阅(~)
- 全项目安全审查: 三轮(~)
- 架构重构审查: 一轮()

### 10.4 遇到的问题及解决

| 问题 | 解决 | 提交 |
|------|------|------|
| Docker PostgreSQL host名不在宿主机解析 | 去Docker化, PG本机安装 | `0ff4987` |
| bcrypt版本不兼容(passlib+bcrypt 5.0) | 降级bcrypt到4.2.1 | `pms_db_backup` |
| .xlsx MIME检测返回None | 扩展名回退+文件头魔数校验 | `e815f58` `14e49e3` |
| SQLAlchemy枚举大小写(SUPER_ADMIN≠super_admin) | PG枚举加入大写标签 | `4e7022d` |
| StaticFiles挂载/截获API路由 | 改用FileResponse手动服务 | `d1084b3` |
| bat文件CMD中文乱码 | ASCII+CRLF编码 | `8c1f5f5` |
| clear路由被{data_id}抢先匹配 | clear路由移到delete前 | `0f982d7` |
| 字段定义upload_schema为空 | 种子脚本补充upload_schema生成 | `2470277` |
| 不同年份Excel格式不同 | 按实际上传文件重新生成Schema | `6c00bb8` `e85c6e8` |

---

## 11. 部署指南

### 11.1 开发环境

1. 安装 PostgreSQL 15-18
2. 创建数据库和用户
3. 复制 `.env.example` → `.env`, 修改配置
4. `pip install -r requirements.txt`
5. `python -m alembic upgrade head`
6. `python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload`
7. 或双击 `start.bat`

### 11.2 生产环境

1. 同上步骤1-5
2. 复制 `.env.production.example` → `.env`
3. 修改: APP_ENV=production, SECRET_KEY=强密钥
4. 前端加 Nginx(HTTPS+SSL)
5. 或双击 `start_prod.bat`(4 worker进程)

### 11.3 Nginx 配置备忘

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;
    
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    client_max_body_size 50M;
    
    location /api/ {
        proxy_pass http://localhost:8000/api/;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 120s;
    }
    
    location / {
        root /path/to/frontend;
        try_files $uri $uri/ /index.html;
    }
    
    add_header X-Frame-Options SAMEORIGIN;
    add_header X-Content-Type-Options nosniff;
}
```

---

> **文档维护**: 本文档基于 93 次 Git 提交、30+ 份项目文档、8000+ 行代码编写。每个功能模块的描述均来自实际代码实现。
