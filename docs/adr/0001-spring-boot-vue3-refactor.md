# ADR-0001: 选用 Spring Boot + Vue 3 技术栈重构

**日期**: 2026-05-27  
**状态**: 已采纳

## 背景

原系统使用 Python FastAPI + Alpine.js 构建，功能完整但存在以下问题：
- 前端无构建工具，代码组织混乱（单文件 app.js 超400行）
- Python 类型检查较弱，大型项目维护困难
- 无ORM迁移管理，数据库变更需手动执行SQL

## 决策

选用 **Spring Boot 3.2 + Vue 3 + Element Plus** 技术栈重构。

## 理由

1. Spring Boot 是Java生态最成熟的企业级框架，类型安全、事务管理、安全认证开箱即用
2. MyBatis-Plus 提供分页、自动填充、逻辑删除等实用功能
3. Vue 3 + Element Plus 组件丰富，开发效率高
4. JWT 无状态认证，易于水平扩展
5. EasyExcel 流式读写，大数据量不溢出

## 后果

- 需要 JDK 17 + Maven + Node.js 开发环境
- 放弃了 Python 灵活的数据处理能力（pandas），改用 EasyExcel
- 数据库从 PostgreSQL 切换为 MySQL，JSONB → JSON 类型
- 前端需要构建步骤（Vite），不能直接HTML运行
