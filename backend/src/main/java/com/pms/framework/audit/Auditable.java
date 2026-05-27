package com.pms.framework.audit;

/**
 * @author ROY
 * @date 2026/05/27
 */

import java.lang.annotation.*;

/**
 * 审计日志注解 — 标记需要自动记录审计日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    /** 操作类型：CREATE/UPDATE/DELETE/UPLOAD/EXPORT/LOGIN */
    String action();
    /** 目标类型：PROJECT/USER/SURVEY_DATA/FILE等 */
    String targetType();
}
