package com.pms.framework.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 简易 IP 速率限制过滤器 — 每IP每分钟最多120次请求
 */
@Slf4j
@Component
public class RateLimitFilter implements Filter {

    private final Cache<String, Integer> counter = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    private static final int MAX_REQUESTS_PER_MINUTE = 120;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // 仅限制 API 请求
        String path = req.getRequestURI();
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(req);
        Integer count = counter.getIfPresent(ip);
        if (count == null) {
            counter.put(ip, 1);
        } else if (count >= MAX_REQUESTS_PER_MINUTE) {
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"code\":429,\"msg\":\"请求过于频繁，请稍后再试\"}");
            log.warn("速率限制触发：IP={}, 请求数={}", ip, count);
            return;
        } else {
            counter.put(ip, count + 1);
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
