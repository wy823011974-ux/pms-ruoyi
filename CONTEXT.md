# 项目管理系统 v2.0 — 领域术语表

> 最后更新: 2026-05-27

## 业务领域

| 术语 | 英文 | 说明 |
|------|------|------|
| 项目 | Project | 一次市场调查任务，如"2024年昌江县体育场地普查" |
| 项目类型 | Project Type | 项目分类：体育场地常态化普查、体育锻炼达标测验、国民体质监测 |
| 项目编码 | Project Code | 自动生成：`HBXM-{类型编码}-{年份}-{序号}` |
| 项目状态 | Project Status | 进行中(IN_PROGRESS) → 已完结(COMPLETED) → 已归档(ARCHIVED) |
| 文件类型配置 | File Type Config | 定义每种项目类型需要哪些Excel文件、Sheet名称、跳过行数 |
| 字段定义 | Field Definition | 定义Excel每一列的名称、类型(text/number/date/select)、必填、脱敏规则 |
| 调查数据 | Survey Data | 从Excel上传的业务数据，已脱敏，以JSON格式存储 |
| 上传历史 | Upload History | 记录每次数据上传的结果（文件名、行数、成功/失败） |
| 文件记录 | File Record | 上传的原始文件（Excel/PDF/Word等），支持版本管理 |
| Schema | Schema | 项目类型+文件类型+字段的完整配置JSON，支持导入/导出 |

## 技术领域

| 术语 | 说明 |
|------|------|
| JWT | JSON Web Token，无状态认证令牌，有效期480分钟 |
| MyBatis-Plus | MyBatis增强工具，提供分页、自动填充、逻辑删除 |
| EasyExcel | 阿里开源Excel读写库，流式处理避免内存溢出 |
| Hutool | 国产Java工具库，提供拼音转换、JSON处理等 |
| Element Plus | Vue 3组件库，提供表格、表单、弹窗等UI组件 |
| Pinia | Vue 3状态管理库 |

## 用户角色

| 角色 | 编码 | 权限 |
|------|------|------|
| 超级管理员 | super_admin | 全部权限，不可删除 |
| 管理员 | admin | 用户管理、项目类型配置 |
| 负责人 | manager | 项目创建、数据上传 |
| 成员 | member | 数据查看、文件上传 |
| 客户 | customer | 仅查看 |

## 脱敏规则

| 规则 | 字段特征 | 示例 |
|------|----------|------|
| name | key含 `xing_ming` 或 `name` | 张三 → 张* |
| id_card | key含 `shen_fen` 或 `id_card` | 110101199003071234 → 110101********1234 |
| phone | key含 `shou_ji`/`phone`/`dian_hua`/`tel` | 13800138001 → 138****8001 |
| address | key含 `zhu_zhi`/`address`/`di_zhi` | 海南省海口市龙华区**** |
