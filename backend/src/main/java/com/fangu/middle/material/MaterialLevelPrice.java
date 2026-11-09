package com.fangu.middle.material;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("material_level_price")
public class MaterialLevelPrice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private String levelCode;
    private BigDecimal salePrice;
}
