package com.fangu.middle.sale;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 采购单表头（复用供货单据表 sale_order） */
@Data
@TableName("sale_order")
public class PurchaseOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long storeId;
    private String storeTypeSnapshot;
    private Long franchiseeIdSnapshot;
    private LocalDate orderDate;
    private BigDecimal totalAmount;
    /** PENDING 待审批 / CONFIRMED 已通过 / CANCELLED 已驳回 */
    private String status;
    private String voucherUrl;
    private String remark;
    private OffsetDateTime confirmedAt;
    private String cancelReason;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
