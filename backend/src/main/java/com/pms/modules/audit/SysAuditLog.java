package com.pms.modules.audit;

/**
 * @author ROY
 * @date 2026/05/27
 */

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审计日志实体 — 记录所有关键操作，保留1年+
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 操作用户ID */
    private Long userId;
    /** 操作类型：CREATE/UPDATE/DELETE/UPLOAD/EXPORT/LOGIN */
    private String action;
    /** 目标类型：PROJECT/USER/SURVEY_DATA/FILE等 */
    private String targetType;
    /** 目标ID */
    private Long targetId;
    /** 操作详情（JSON格式） */
    private String detail;
    /** 客户端IP地址 */
    private String ipAddress;
    /** 操作结果：success/failed */
    private String result;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
