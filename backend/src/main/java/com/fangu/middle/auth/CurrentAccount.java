package com.fangu.middle.auth;

import lombok.Data;

/** 当前账号上下文：账号 + 角色 + 数据范围 + 归属 */
@Data
public class CurrentAccount {
    private Long userId;
    private String username;
    private String roleCode;
    private String dataScope;   // ALL / FRANCHISEE / STORE
    private Long franchiseeId;
    private Long storeId;
}
