package com.fangu.middle.datascope;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.fangu.middle.auth.CurrentAccount;
import com.fangu.middle.common.BizException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;

import java.sql.Connection;
import java.util.Map;
import java.util.Set;

/**
 * 数据权限拦截器：在 SQL 生成阶段（而非业务逻辑阶段）追加归属条件。
 * 仅当当前线程持有 DataScopeContext（由 @DataScope 切面写入）且账号范围非 ALL 时生效。
 *
 * 列映射约定（需求文档 3.2）：
 *   FRANCHISEE -> 业务表 franchisee_id / 加盟商表 id / 销货单 franchisee_id_snapshot
 *   STORE      -> 业务表 store_id / 门店表 id
 * 字典表（franchise_level、material）范围内账号只读放行，写操作拒绝。
 * 未登记的表一律 fail-closed 抛 403，避免漏写过滤造成越权。
 */
public class DataScopeInterceptor implements InnerInterceptor {

    /** FRANCHISEE 范围：表 -> 归属列 */
    private static final Map<String, String> FRANCHISEE_COLUMN = Map.of(
            "franchisee", "id",
            "store", "franchisee_id",
            "sale_order", "franchisee_id_snapshot"
    );

    /** STORE 范围：表 -> 归属列 */
    private static final Map<String, String> STORE_COLUMN = Map.of(
            "store", "id",
            "sale_order", "store_id",
            "store_stock", "store_id",
            "stock_flow", "store_id"
    );

    /**
     * 范围内账号可只读访问的字典/明细表：
     * material 为公共商品字典；sale_order_item 无归属列，访问前必须先通过订单头的归属校验。
     */
    private static final Set<String> READABLE_DICT = Set.of("franchise_level", "material", "sale_order_item");

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        String newSql = rewrite(boundSql.getSql(), false);
        if (newSql != null) {
            PluginUtils.mpBoundSql(boundSql).sql(newSql);
        }
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(sh);
        SqlCommandType type = mpSh.mappedStatement().getSqlCommandType();
        if (type != SqlCommandType.UPDATE && type != SqlCommandType.DELETE) {
            return;
        }
        BoundSql boundSql = mpSh.boundSql();
        String newSql = rewrite(boundSql.getSql(), true);
        if (newSql != null) {
            PluginUtils.mpBoundSql(boundSql).sql(newSql);
        }
    }

    /** 返回改写后的 SQL；无需改写返回 null */
    private String rewrite(String sql, boolean write) {
        CurrentAccount acc = DataScopeContext.get();
        if (acc == null || "ALL".equals(acc.getDataScope())) {
            return null;
        }
        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (stmt instanceof Select select) {
                PlainSelect ps = select.getPlainSelect();
                if (ps == null || !(ps.getFromItem() instanceof Table table)) {
                    throw new BizException(403, "越权访问：数据范围未覆盖该查询");
                }
                String tableName = table.getName().toLowerCase();
                if (READABLE_DICT.contains(tableName)) {
                    return null;
                }
                String column = scopeColumn(acc, tableName);
                Long value = scopeValue(acc);
                ps.setWhere(and(ps.getWhere(), new EqualsTo(new Column(column), new LongValue(value))));
                return select.toString();
            }
            if (stmt instanceof Update update) {
                String tableName = update.getTable().getName().toLowerCase();
                String column = scopeColumn(acc, tableName);
                Long value = scopeValue(acc);
                update.setWhere(and(update.getWhere(), new EqualsTo(new Column(column), new LongValue(value))));
                return update.toString();
            }
            if (stmt instanceof Delete delete) {
                String tableName = delete.getTable().getName().toLowerCase();
                String column = scopeColumn(acc, tableName);
                Long value = scopeValue(acc);
                delete.setWhere(and(delete.getWhere(), new EqualsTo(new Column(column), new LongValue(value))));
                return delete.toString();
            }
            throw new BizException(403, "越权访问：数据范围未覆盖该操作");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(500, "数据权限 SQL 解析失败");
        }
    }

    private String scopeColumn(CurrentAccount acc, String tableName) {
        Map<String, String> mapping = "FRANCHISEE".equals(acc.getDataScope()) ? FRANCHISEE_COLUMN : STORE_COLUMN;
        String column = mapping.get(tableName);
        if (column == null) {
            throw new BizException(403, "越权访问：数据范围未覆盖该资源");
        }
        return column;
    }

    private Long scopeValue(CurrentAccount acc) {
        Long value = "FRANCHISEE".equals(acc.getDataScope()) ? acc.getFranchiseeId() : acc.getStoreId();
        if (value == null) {
            throw new BizException(403, "账号未绑定归属组织");
        }
        return value;
    }

    private Expression and(Expression where, Expression extra) {
        return where == null ? extra : new AndExpression(where, extra);
    }
}
