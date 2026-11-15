package com.fangu.middle.writeoff;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 核销记录行 */
@Data
public class WriteoffVO {
    private Long id;
    private String writeoffNo;
    private Long packageId;
    private String packageName;
    private Long storeId;
    private String storeName;
    private String beautician;
    private BigDecimal packagePrice;
    private String remark;
    private Long itemCount;
    private BigDecimal totalQty;
    private OffsetDateTime createdAt;
}
