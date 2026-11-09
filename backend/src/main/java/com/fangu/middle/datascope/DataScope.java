package com.fangu.middle.datascope;

import java.lang.annotation.*;

/**
 * 标记接口需要数据范围过滤。
 * scope 为 ALL（默认）表示不做范围类型限制，按账号自身范围过滤；
 * scope 为 FRANCHISEE/STORE 表示该接口仅允许对应范围的账号访问，范围不匹配返回 403。
 * 总部账号（dataScope=ALL）不追加任何条件。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    ScopeType scope() default ScopeType.ALL;

    enum ScopeType { ALL, FRANCHISEE, STORE }
}
