package com.fangu.middle.pkg;

import lombok.Data;

import java.math.BigDecimal;

/** 套餐商品明细 / 可绑定商品行 */
@Data
public class PackageItemVO {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private BigDecimal qty;
    private String itemType;
    private BigDecimal currentQty;
}
