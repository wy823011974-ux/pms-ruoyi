-- ============================================
-- 项目管理系统 v2.0 - RuoYi-Vue3 Edition
-- 数据库初始化脚本 (MySQL 8.0)
-- ============================================

-- 强制客户端使用 UTF-8 编码，避免中文乱码
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE DATABASE IF NOT EXISTS pms_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pms_db;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名(手机号)',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT '密码(bcrypt)',
    display_name VARCHAR(50) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',
    department VARCHAR(100) COMMENT '所属部门/单位',
    role VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT '角色: super_admin/admin/manager/member/customer',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    login_fail_count INT DEFAULT 0 COMMENT '登录失败次数',
    lock_until DATETIME COMMENT '锁定截止时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '用户表';

-- 审计日志表
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL COMMENT '操作用户ID(登录失败时为NULL)',
    action VARCHAR(50) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(50) NOT NULL COMMENT '目标类型',
    target_id BIGINT COMMENT '目标ID',
    detail JSON COMMENT '详情',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    result VARCHAR(20) NOT NULL DEFAULT 'success' COMMENT '结果',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action)
) COMMENT '审计日志';

-- 项目类型表
CREATE TABLE IF NOT EXISTS pms_project_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '类型名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '类型编码',
    description VARCHAR(500) COMMENT '描述',
    create_by BIGINT COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '项目类型';

-- 文件类型配置表
CREATE TABLE IF NOT EXISTS pms_file_type_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_type_id BIGINT NOT NULL COMMENT '所属项目类型',
    name VARCHAR(100) NOT NULL COMMENT '文件类型名称',
    code VARCHAR(50) NOT NULL COMMENT '编码(拼音自动生成)',
    skip_rows INT DEFAULT 0 COMMENT '跳过表头行数',
    sheet_name VARCHAR(100) COMMENT 'Excel Sheet名称(NULL=第一个)',
    has_fields TINYINT DEFAULT 0 COMMENT '是否配置字段校验',
    upload_schema JSON COMMENT '上传Schema JSON',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_type_code (project_type_id, code),
    INDEX idx_project_type (project_type_id)
) COMMENT '文件类型配置';

-- 字段定义表
CREATE TABLE IF NOT EXISTS pms_field_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_type_config_id BIGINT NOT NULL COMMENT '所属文件类型配置',
    field_key VARCHAR(50) NOT NULL COMMENT '字段标识(pinyin)',
    field_label VARCHAR(100) NOT NULL COMMENT '字段名称(中文)',
    field_type VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '类型: text/number/date/select',
    options JSON COMMENT '选项列表(select类型)',
    is_required TINYINT DEFAULT 0 COMMENT '是否必填',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_active TINYINT DEFAULT 1 COMMENT '是否启用',
    extra_attrs JSON COMMENT '额外属性(校验规则/脱敏配置)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ftc (file_type_config_id)
) COMMENT '字段定义';

-- 项目表
CREATE TABLE IF NOT EXISTS pms_project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '项目编号',
    name VARCHAR(200) NOT NULL COMMENT '项目名称',
    year INT NOT NULL COMMENT '调查年份',
    location VARCHAR(200) NOT NULL COMMENT '调查地区',
    project_type_id BIGINT NOT NULL COMMENT '项目类型',
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态: IN_PROGRESS/COMPLETED/ARCHIVED',
    leader_id BIGINT COMMENT '项目负责人',
    creator_id BIGINT NOT NULL COMMENT '创建人',
    dynamic_fields JSON COMMENT '动态字段值',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    is_deleted TINYINT DEFAULT 0 COMMENT '软删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (project_type_id),
    INDEX idx_status (status),
    INDEX idx_year (year)
) COMMENT '项目表';

-- 文件记录表
CREATE TABLE IF NOT EXISTS pms_file_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '所属项目',
    file_type_config_id BIGINT COMMENT '文件类型配置',
    filename VARCHAR(500) NOT NULL COMMENT '存储文件名',
    original_name VARCHAR(500) NOT NULL COMMENT '原始文件名',
    file_type VARCHAR(50) COMMENT '文件类型code',
    version INT DEFAULT 1 COMMENT '版本号',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    skip_rows INT DEFAULT 0 COMMENT '跳过行数',
    is_overwrite TINYINT DEFAULT 0 COMMENT '是否覆盖上传',
    storage_path VARCHAR(1000) COMMENT '存储路径',
    upload_by BIGINT COMMENT '上传人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project (project_id),
    INDEX idx_ftc (file_type_config_id)
) COMMENT '文件记录';

-- 调查数据表
CREATE TABLE IF NOT EXISTS pms_survey_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '所属项目',
    file_type_config_id BIGINT COMMENT '文件类型配置',
    row_data JSON NOT NULL COMMENT '脱敏后的数据行',
    project_code VARCHAR(50) COMMENT '项目编号(冗余)',
    project_name VARCHAR(200) COMMENT '项目名称(冗余)',
    project_year INT COMMENT '年份(冗余)',
    project_location VARCHAR(200) COMMENT '地区(冗余)',
    project_type_name VARCHAR(100) COMMENT '项目类型(冗余)',
    file_type_name VARCHAR(100) COMMENT '文件类型(冗余)',
    original_filename VARCHAR(500) COMMENT '原始文件名',
    upload_by BIGINT COMMENT '上传人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project (project_id),
    INDEX idx_ftc (file_type_config_id),
    INDEX idx_pcode (project_code),
    INDEX idx_pyear (project_year)
) COMMENT '调查数据';

-- 上传历史表
CREATE TABLE IF NOT EXISTS pms_upload_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '所属项目',
    file_type_config_id BIGINT COMMENT '文件类型配置',
    upload_by BIGINT NOT NULL COMMENT '上传人',
    original_filename VARCHAR(500) NOT NULL COMMENT '原始文件名',
    total_rows INT DEFAULT 0 COMMENT '总行数',
    success_rows INT DEFAULT 0 COMMENT '成功行数',
    status VARCHAR(20) DEFAULT 'success' COMMENT '状态: success/failed',
    error_detail JSON COMMENT '错误详情',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project (project_id)
) COMMENT '上传历史';

-- 插入默认超级管理员 (密码: admin123)
INSERT INTO sys_user (username, email, password, display_name, phone, department, role, status)
VALUES ('admin', 'admin@pms.local', '$2b$10$TMSM1YpVj2C8XrBfaLJQF.C5isMlwxA62vXHtseAUhTZxAnRewxrS', '系统管理员', '13800000000', '技术部', 'super_admin', 1)
ON DUPLICATE KEY UPDATE username=username;
