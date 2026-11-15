package com.fangu.middle.writeoff;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 套餐核销单 */
@Data
@TableName("store_writeoff")
public class StoreWriteoff {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String writeoffNo;
    private Long storeId;
    private Long franchiseeId;
    private Long packageId;
    private String packageName;
    /** 核销时套餐价格快照，用于业绩统计 */
    private BigDecimal packagePrice;
    /** 服务美容师 */
    private String beautician;
    /** 幂等号 */
    private String requestNo;
    private String remark;
    private Long createdBy;
    private OffsetDateTime createdAt;
}
