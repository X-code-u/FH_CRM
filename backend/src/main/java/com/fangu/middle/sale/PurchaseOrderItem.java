package com.fangu.middle.sale;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("sale_order_item")
public class PurchaseOrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long materialId;
    private BigDecimal qty;
    /** 下单时价格快照，审批改量不重新定价（规则 R2） */
    private BigDecimal price;
    private BigDecimal amount;
    private String priceSource;
}
