package com.fangu.middle.datascope;

import com.fangu.middle.audit.AuditService;
import com.fangu.middle.auth.AuthService;
import com.fangu.middle.auth.CurrentAccount;
import com.fangu.middle.common.BizException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * @DataScope 切面：把当前账号写入数据权限上下文，供 MyBatis 拦截器在 SQL 层追加归属条件。
 * 注解声明的范围与账号范围不匹配时返回 403 并写审计日志。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class DataScopeAspect {

    private final AuthService authService;
    private final AuditService auditService;

    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint pjp, DataScope dataScope) throws Throwable {
        CurrentAccount acc = authService.current();
        if (!"ALL".equals(acc.getDataScope())) {
            DataScope.ScopeType declared = dataScope.scope();
            if (declared != DataScope.ScopeType.ALL && !declared.name().equals(acc.getDataScope())) {
                String uri = ((MethodSignature) pjp.getSignature()).getMethod().getName();
                auditService.recordDeny("账号 " + acc.getUsername() + " 越权访问 " + uri
                        + "（需要范围 " + declared + "，实际范围 " + acc.getDataScope() + "）");
                throw new BizException(403, "无权限或越权");
            }
            DataScopeContext.set(acc);
        }
        try {
            return pjp.proceed();
        } finally {
            DataScopeContext.clear();
        }
    }
}
