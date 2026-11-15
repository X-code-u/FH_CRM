package com.fangu.middle.pkg;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 套餐列表行 */
@Data
public class PackageVO {
    private Long id;
    private Long storeId;
    private String storeName;
    private String packageName;
    private Integer durationMinutes;
    private BigDecimal price;
    private String status;
    private String remark;
    private Long itemCount;
    private OffsetDateTime updatedAt;
}
