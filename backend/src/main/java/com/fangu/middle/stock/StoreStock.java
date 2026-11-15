package com.fangu.middle.stock;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 门店库存结存：(store_id, material_id) 唯一 */
@Data
@TableName("store_stock")
public class StoreStock {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private Long materialId;
    private BigDecimal currentQty;
    private OffsetDateTime updatedAt;
}
