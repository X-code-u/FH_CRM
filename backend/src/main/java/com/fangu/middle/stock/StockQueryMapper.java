package com.fangu.middle.stock;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StockQueryMapper {

    /** 门店库存台账：主表 store_stock，数据权限拦截器按 store_id 追加归属条件 */
    @Select("<script>"
            + "SELECT ss.store_id AS storeId, s.store_code AS storeCode, s.name AS storeName, "
            + "ss.material_id AS materialId, m.code AS materialCode, m.name AS materialName, "
            + "m.spec AS spec, m.unit AS unit, ss.current_qty AS currentQty, "
            + "m.warn_min_qty AS warnMinQty, ss.updated_at AS updatedAt "
            + "FROM store_stock ss "
            + "LEFT JOIN store s ON s.id = ss.store_id "
            + "LEFT JOIN material m ON m.id = ss.material_id "
            + "WHERE 1 = 1 "
            + "<if test='q.storeId != null'>AND ss.store_id = #{q.storeId} </if>"
            + "<if test='q.storeName != null and q.storeName != \"\"'>AND s.name LIKE CONCAT('%', #{q.storeName}, '%') </if>"
            + "<if test='q.materialName != null and q.materialName != \"\"'>AND m.name LIKE CONCAT('%', #{q.materialName}, '%') </if>"
            + "ORDER BY ss.updated_at DESC, ss.id DESC"
            + "</script>")
    IPage<StockVO> selectStockPage(IPage<StockVO> page, @Param("q") StockController.StockQuery q);
}
