package com.fangu.middle.material;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface MaterialMapper extends BaseMapper<Material> {

    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(code, 2) AS INTEGER)), 0) FROM material")
    int maxCodeSeq();

    /**
     * 库存扣减：仅有限库存参与，条件更新兜底超卖（规则 R3 原子自增/自减思路）。
     * 返回 0 表示库存不足或库存为无限（无需扣减由业务层先行判断）。
     */
    @Update("UPDATE material SET stock_qty = stock_qty - #{qty}, updated_at = now() "
            + "WHERE id = #{id} AND stock_qty IS NOT NULL AND stock_qty >= #{qty}")
    int deductStock(@Param("id") Long id, @Param("qty") BigDecimal qty);
}
