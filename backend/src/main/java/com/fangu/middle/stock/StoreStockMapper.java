package com.fangu.middle.stock;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface StoreStockMapper extends BaseMapper<StoreStock> {

    /** 原子自增，避免并发读改写 */
    @Update("UPDATE store_stock SET current_qty = current_qty + #{qty}, updated_at = now() "
            + "WHERE store_id = #{storeId} AND material_id = #{materialId}")
    int addQty(@Param("storeId") Long storeId, @Param("materialId") Long materialId,
               @Param("qty") BigDecimal qty);

    @Insert("INSERT INTO store_stock(store_id, material_id, current_qty, updated_at) "
            + "VALUES(#{storeId}, #{materialId}, #{qty}, now())")
    int insertRow(@Param("storeId") Long storeId, @Param("materialId") Long materialId,
                  @Param("qty") BigDecimal qty);

    /** 原子扣减，结存不足时不更新（返回 0） */
    @Update("UPDATE store_stock SET current_qty = current_qty - #{qty}, updated_at = now() "
            + "WHERE store_id = #{storeId} AND material_id = #{materialId} AND current_qty >= #{qty}")
    int deductQty(@Param("storeId") Long storeId, @Param("materialId") Long materialId,
                  @Param("qty") BigDecimal qty);
}
