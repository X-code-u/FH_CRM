package com.fangu.middle.audit;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangu.middle.common.PageResult;
import com.fangu.middle.common.R;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 审计日志查询：仅总部超管 */
@RestController
@RequestMapping("/audit-log")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogMapper auditLogMapper;

    @SaCheckRole("HQ_ADMIN")
    @GetMapping("/page")
    public R<PageResult<AuditLog>> page(AuditQuery query) {
        Page<AuditLog> page = auditLogMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<AuditLog>lambdaQuery()
                        .eq(query.getOperateType() != null && !query.getOperateType().isBlank(),
                                AuditLog::getOperateType, query.getOperateType())
                        .like(query.getUsername() != null && !query.getUsername().isBlank(),
                                AuditLog::getUsername, query.getUsername())
                        .orderByDesc(AuditLog::getId));
        return R.ok(PageResult.of(page));
    }

    @Data
    public static class AuditQuery {
        private long pageNum = 1;
        private long pageSize = 10;
        private String operateType;
        private String username;
    }
}
