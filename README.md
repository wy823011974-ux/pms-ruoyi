# 项目管理系统 (RuoYi-Vue3版)

基于 **Spring Boot 3.2 + Vue 3 + Element Plus** 的市场调查项目管理系统。

## 技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| 后端 | Spring Boot + MyBatis-Plus | 3.2 + 3.5.6 |
| 安全 | Spring Security + JWT | 0.12.5 |
| 数据库 | MySQL 8.0 + Redis 7 | Docker |
| 前端 | Vue 3 + Element Plus + Pinia | 3.4 + 2.7 + 2.1 |
| 构建 | Maven + Vite | 3.9 + 5.2 |
| Excel | EasyExcel | 3.3.4 |

## 快速启动

```bash
# 1. 启动基础设施
docker compose up -d mysql redis

# 2. 启动后端
cd backend && mvn spring-boot:run

# 3. 启动前端
cd frontend && npm install && npm run dev
```

默认管理员：`admin` / `admin123`

## 功能模块

- 仪表盘 — 统计概览
- 项目管理 — CRUD + 状态流转 + 编码自动生成
- 项目类型配置 — 文件类型 + 字段定义 + Schema导入导出
- 调查数据管理 — Excel上传 + 校验 + 脱敏 + 导出
- 文件管理 — 上传 + 版本管理 + 下载
- 用户管理 — 5角色体系 + 审批

## 项目文档

- [领域术语表](CONTEXT.md)
- [系统使用说明](docs/user-manual.md)
- [开发日志](docs/devlog-2026-05-27.md)
- [测试日志](docs/test-log.md)
- [架构决策记录](docs/adr/)
