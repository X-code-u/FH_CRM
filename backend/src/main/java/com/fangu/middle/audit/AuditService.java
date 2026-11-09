package com.fangu.middle.audit;

import cn.dev33.satoken.stp.StpUtil;
import com.fangu.middle.auth.CurrentAccount;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogMapper auditLogMapper;

    /** 记录一次业务变更；审计写入失败不阻断主流程 */
    public void record(String type, String oldValue, String newValue) {
        try {
            AuditLog logRow = new AuditLog();
            fillOperator(logRow);
            logRow.setOperateType(type);
            logRow.setOldValue(oldValue);
            logRow.setNewValue(newValue);
            auditLogMapper.insert(logRow);
        } catch (Exception e) {
            log.warn("审计日志写入失败: {}", e.getMessage());
        }
    }

    /** 越权访问留痕（需求文档 3.3：越权返回 403 并写审计日志） */
    public void recordDeny(String content) {
        try {
            AuditLog logRow = new AuditLog();
            fillOperator(logRow);
            logRow.setOperateType("ACCESS_DENY");
            logRow.setContent(content);
            auditLogMapper.insert(logRow);
        } catch (Exception e) {
            log.warn("审计日志写入失败: {}", e.getMessage());
        }
    }

    private void fillOperator(AuditLog logRow) {
        try {
            CurrentAccount acc = (CurrentAccount) StpUtil.getSession().get("account");
            if (acc != null) {
                logRow.setUserId(acc.getUserId());
                logRow.setUsername(acc.getUsername());
            }
        } catch (Exception ignored) {
            // 未登录场景（如登录失败审计）不填充操作人
        }
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            logRow.setIp(request.getRemoteAddr());
        }
    }
}
