package com.fangu.middle.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/** @Audit 切面：方法成功执行后落审计日志，旧值来自 AuditContext，新值为方法入参 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint pjp, Audit audit) throws Throwable {
        try {
            Object result = pjp.proceed();
            auditService.record(audit.type(), AuditContext.getOldValue(), argsJson(pjp.getArgs()));
            return result;
        } finally {
            AuditContext.clear();
        }
    }

    private String argsJson(Object[] args) {
        try {
            return objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            return String.valueOf(args);
        }
    }
}
