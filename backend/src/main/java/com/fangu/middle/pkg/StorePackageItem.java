package com.fangu.middle.pkg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 套餐绑定的入库商品明细（store_id/franchisee_id 冗余存储，供数据权限过滤） */
@Data
@TableName("store_package_item")
public class StorePackageItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long packageId;
    private Long storeId;
    private Long franchiseeId;
    private Long materialId;
    private BigDecimal qty;
    /** FIXED 固定消耗（核销必扣） / OPTIONAL 可选（核销时选择） */
    private String itemType;
    private OffsetDateTime createdAt;
}
