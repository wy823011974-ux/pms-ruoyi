# Task Plan: PMS RuoYi-Vue3 重构项目

## Goal
将原有 FastAPI+Alpine.js 项目管理系统重构为 Spring Boot 3.2 + Vue 3 + Element Plus 架构，实现功能对齐并打磨 Apple 风格 UI。

## Current Phase
Phase 8 — Apple UI 打磨完成，待收尾验证

## Phases

### Phase 1: 基础设施修复 ✅
- [x] .gitignore 完善
- [x] MyBatisPlusConfig 分页插件
- [x] Result.java 泛型修复
- [x] SecurityConfig @EnableMethodSecurity
- [x] JwtAuthFilter 角色注入
- [x] Maven 编译通过
- **Status:** complete

### Phase 2: Service 层重构 ✅
- [x] SurveyService — Excel解析/校验/脱敏/入库/导出
- [x] ProjectService — 编码生成/状态流转验证
- [x] Controller 重构使用 Service
- **Status:** complete

### Phase 3: 后端缺失功能 ✅
- [x] 审计日志 AOP (@Auditable + AuditLogAspect)
- [x] Schema 导入导出 (ProjectTypeController)
- [x] 文件下载端点
- [x] 修改密码 API
- [x] 速率限制 (RateLimitFilter)
- **Status:** complete

### Phase 4: 前端页面完善 ✅
- [x] Survey 页完整重构（表格/导出/历史）
- [x] File 页重构（下载/版本）
- [x] Dashboard 增强
- [x] ProjectType Schema 导入导出 UI
- [x] 修改密码弹窗对接 API
- **Status:** complete

### Phase 5: 安全加固 ✅
- [x] RateLimitFilter (Caffeine)
- [x] @PreAuthorize 权限检查
- [x] JWT 权限加载修复
- **Status:** complete

### Phase 6: 测试 ✅
- [x] AnonymizerTest (13 tests)
- [x] CommonTest (7 tests)
- [x] ExcelValidatorTest (3 tests)
- [x] 23 tests pass
- **Status:** complete

### Phase 7: 文档 ✅
- [x] CONTEXT.md 领域术语表
- [x] 3 ADR (技术栈/JSON列/AOP审计)
- [x] 开发日志/测试日志/用户手册
- [x] DESIGN-APPLE.md Apple 设计文档
- **Status:** complete

### Phase 8: Apple HIG 设计重构 ✅
- [x] DESIGN-APPLE.md 设计文档
- [x] 全局 CSS: Apple 蓝 #007AFF + iOS 灰背景 + SF 字体
- [x] 侧边栏: 毛玻璃 backdrop-filter
- [x] 登录页: 20px 圆角白卡
- [x] 仪表盘: 四色统计卡片 + 列表式快捷操作
- [x] 项目管理页: Apple 卡片风格
- [x] 调查数据页: 宽松布局 + 上传控件
- [x] 文件管理页: Apple 风格
- [x] 用户管理页: 列宽优化 + 下拉菜单
- [x] 项目类型配置页: 全部重写
- [x] 输入框椭圆统一 (wrapper + inner 24px)
- [x] 按钮胶囊形统一 (24px)
- [x] el-link underline API 修复
- **Status:** complete

### Phase 9: 代码审查与修正 ✅
- [x] 5 角度全面代码审查（40+ 发现）
- [x] 速率限制原子操作修复
- [x] 级联删除 @Transactional 添加
- [x] 401 穿透修复
- [x] 登录失败锁定机制实现
- [x] RuntimeException → BusinessException
- [x] 异常信息不再泄露
- [x] skipRows 参数名匹配修复
- [x] fetchUserInfo 温和降级
- [x] 路由角色守卫
- [x] creatorId 从认证上下文获取
- [x] isDeleted 加入 VO
- **Status:** complete

### Phase 10: 阿里巴巴 Java 规范审查 ✅
- [x] 全部 39 类添加 @author
- [x] 空 catch 加注释
- [x] MyBatis StdOutImpl → Slf4jImpl
- [x] SQL 表补 update_time
- [x] BusinessException 替代 RuntimeException
- [x] JWT secret 支持环境变量
- **Status:** complete

### Phase 11: 收尾验证
- [ ] 全流程手动测试（登录→创建项目类型→导入Schema→创建项目→上传数据→导出→删除）
- [ ] 后端 mvn test 全绿确认
- [ ] 前端 vite build 无报错
- [ ] 浏览器 console 无 error
- [ ] Docker 环境启动验证
- **Status:** pending

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| 调查数据用 JSON 列存储 | 不同文件类型列结构不同，无法固定列 |
| AOP 审计日志 | 非侵入式，统一处理 |
| Restrained 色彩策略 | Product Register 管理后台 |
| Apple HIG 设计语言 | 用户明确要求，办公场景长时间使用需减少视觉疲劳 |
| BusinessException 替代 RuntimeException | Alibaba 强制规约 |

## Errors Encountered
| Error | Resolution |
|-------|------------|
| EasyExcel skipRows 失效 | 添加 headRowNumber(0) 禁用自动表头 |
| 脱敏中文列名不匹配 | 改用字段定义 extra_attrs 匹配 |
| 导出 blob 被 JSON 拦截 | 拦截器跳过 blob 类型响应 |
| 数据库中文乱码 | SQL SET NAMES utf8mb4 + JDBC UTF-8 |
| hasFields 布尔反序列化报错 | Integer 改 Boolean |
| 登录锁定机制形同虚设 | 补全 failCount 递增和 lock_until 设置 |
| 前后端 skipRows 参数名蛇形/驼峰不匹配 | 添加 @RequestParam name="skip_rows" |
