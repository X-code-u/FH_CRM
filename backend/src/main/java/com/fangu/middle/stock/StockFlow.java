package com.fangu.middle.stock;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 门店出入库流水 */
@Data
@TableName("stock_flow")
public class StockFlow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private Long materialId;
    /** SALE_IN 采购入库 */
    private String flowType;
    private Long relateOrderId;
    /** 幂等号 */
    private String requestNo;
    private BigDecimal qty;
    private OffsetDateTime createdAt;
}
