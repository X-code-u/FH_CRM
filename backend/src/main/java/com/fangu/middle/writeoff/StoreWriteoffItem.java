package com.fangu.middle.writeoff;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 核销明细（实际扣减的商品与数量） */
@Data
@TableName("store_writeoff_item")
public class StoreWriteoffItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long writeoffId;
    private Long storeId;
    private Long franchiseeId;
    private Long materialId;
    private BigDecimal qty;
    /** FIXED 固定消耗 / OPTIONAL 可选 */
    private String itemType;
    private OffsetDateTime createdAt;
}
