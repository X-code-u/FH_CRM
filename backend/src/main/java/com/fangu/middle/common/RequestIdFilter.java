package com.fangu.middle.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.UUID;

/** 为每个请求生成 requestId，写入响应体 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, java.io.IOException {
        RequestIdHolder.set(UUID.randomUUID().toString());
        try {
            chain.doFilter(req, res);
        } finally {
            RequestIdHolder.clear();
        }
    }
}
