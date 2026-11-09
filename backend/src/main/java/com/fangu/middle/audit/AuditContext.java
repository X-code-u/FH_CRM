package com.fangu.middle.audit;

/** 业务层在 @Audit 方法内写入变更前旧值，切面落库后清除 */
public final class AuditContext {

    private static final ThreadLocal<String> OLD_VALUE = new ThreadLocal<>();

    public static void setOldValue(String oldValue) { OLD_VALUE.set(oldValue); }
    public static String getOldValue() { return OLD_VALUE.get(); }
    public static void clear() { OLD_VALUE.remove(); }

    private AuditContext() {}
}
