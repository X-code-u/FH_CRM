package com.fangu.middle.stock;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 门店库存台账行（结存 + 物料信息） */
@Data
public class StockVO {
    private Long storeId;
    private String storeCode;
    private String storeName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private BigDecimal currentQty;
    private BigDecimal warnMinQty;
    private OffsetDateTime updatedAt;
}
