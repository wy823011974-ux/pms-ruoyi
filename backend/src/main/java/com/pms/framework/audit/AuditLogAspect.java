package com.pms.framework.audit;

import cn.hutool.json.JSONUtil;
import com.pms.modules.audit.SysAuditLog;
import com.pms.modules.audit.SysAuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * 审计日志切面 — 自动拦截 @Auditable 注解的方法并记录操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final SysAuditLogMapper auditLogMapper;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setAction(auditable.action());
        auditLog.setTargetType(auditable.targetType());

        // 获取当前用户ID
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            auditLog.setUserId((Long) auth.getPrincipal());
        }

        // 获取客户端IP
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            auditLog.setIpAddress(getClientIp(request));
        }

        // 提取方法参数作为详情
        Map<String, Object> params = new HashMap<>();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();
        for (int i = 0; i < paramNames.length; i++) {
            if (paramValues[i] != null && !(paramValues[i] instanceof jakarta.servlet.http.HttpServletRequest)
                    && !(paramValues[i] instanceof jakarta.servlet.http.HttpServletResponse)) {
                try {
                    params.put(paramNames[i], paramValues[i].toString());
                } catch (Exception ignored) {}
            }
        }
        auditLog.setDetail(JSONUtil.toJsonStr(params));

        // 执行目标方法
        try {
            Object result = joinPoint.proceed();
            auditLog.setResult("success");
            return result;
        } catch (Exception e) {
            auditLog.setResult("failed");
            if (auditLog.getDetail() != null) {
                auditLog.setDetail(auditLog.getDetail() + " | error: " + e.getMessage());
            }
            throw e;
        } finally {
            try {
                auditLogMapper.insert(auditLog);
            } catch (Exception e) {
                log.error("审计日志写入失败", e);
            }
        }
    }

    /**
     * 获取客户端真实IP — 支持代理/Nginx
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
