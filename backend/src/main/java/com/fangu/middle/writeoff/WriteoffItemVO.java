package com.fangu.middle.writeoff;

import lombok.Data;

import java.math.BigDecimal;

/** 核销明细行 */
@Data
public class WriteoffItemVO {
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String spec;
    private String unit;
    private BigDecimal qty;
    private String itemType;
}
