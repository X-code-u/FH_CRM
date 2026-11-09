package com.fangu.middle.datascope;

import com.fangu.middle.auth.CurrentAccount;

/**
 * 数据权限上下文：由 Controller 层 AOP 在进入标注 @DataScope 的接口时写入，
 * MyBatis 拦截器读取后在 SQL 生成阶段追加归属条件。
 * 列名映射约定（见需求文档 3.2）：
 *   FRANCHISEE -> store.franchisee_id / sale_order.franchisee_id_snapshot
 *   STORE      -> *.store_id
 */
public final class DataScopeContext {
    private static final ThreadLocal<CurrentAccount> CTX = new ThreadLocal<>();

    public static void set(CurrentAccount account) { CTX.set(account); }
    public static CurrentAccount get() { return CTX.get(); }
    public static void clear() { CTX.remove(); }

    private DataScopeContext() {}
}
