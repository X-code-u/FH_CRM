package com.fangu.middle.material;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@TableName("material")
public class Material {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String spec;
    private String unit;
    private String category;
    private BigDecimal baseSalePrice;
    private BigDecimal directPrice;
    private BigDecimal costPrice;
    private BigDecimal warnMinQty;
    /** 总部可售库存；null 表示无限库存（编辑时需允许写回 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal stockQty;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
