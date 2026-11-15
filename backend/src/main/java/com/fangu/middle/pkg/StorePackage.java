package com.fangu.middle.pkg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 门店套餐（如 90 分钟套餐 / 45 分钟套餐） */
@Data
@TableName("store_package")
public class StorePackage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private Long franchiseeId;
    private String packageName;
    private Integer durationMinutes;
    private BigDecimal price;
    /** ENABLED 启用 / DISABLED 停用 */
    private String status;
    private String remark;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
