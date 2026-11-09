package com.fangu.middle.audit;

import java.lang.annotation.*;

/** 变更留痕：记录操作人、旧值（由 AuditContext 提供）与新值（方法入参） */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audit {
    /** 操作类型，如 LEVEL_SAVE / FRANCHISEE_LEVEL_CHANGE / FRANCHISEE_STATUS_CHANGE / STORE_STATUS_CHANGE */
    String type();
}
