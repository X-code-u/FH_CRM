package com.fangu.middle.pkg;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PackageQueryMapper {

    /** 套餐列表：主表 store_package，数据权限拦截器按 store_id / franchisee_id 追加条件 */
    @Select("<script>"
            + "SELECT p.id, p.store_id AS storeId, s.name AS storeName, p.package_name AS packageName, "
            + "p.duration_minutes AS durationMinutes, p.price, p.status, p.remark, "
            + "(SELECT COUNT(*) FROM store_package_item i WHERE i.package_id = p.id) AS itemCount, "
            + "p.updated_at AS updatedAt "
            + "FROM store_package p LEFT JOIN store s ON s.id = p.store_id "
            + "WHERE 1 = 1 "
            + "<if test='name != null and name != \"\"'>AND p.package_name LIKE CONCAT('%', #{name}, '%') </if>"
            + "<if test='status != null and status != \"\"'>AND p.status = #{status} </if>"
            + "ORDER BY p.id DESC"
            + "</script>")
    IPage<PackageVO> selectPackagePage(IPage<PackageVO> page,
                                       @Param("name") String name,
                                       @Param("status") String status);

    /** 套餐绑定的商品明细 */
    @Select("SELECT i.material_id AS materialId, m.code AS materialCode, m.name AS materialName, "
            + "m.spec AS spec, m.unit AS unit, i.qty AS qty, i.item_type AS itemType "
            + "FROM store_package_item i LEFT JOIN material m ON m.id = i.material_id "
            + "WHERE i.package_id = #{packageId} ORDER BY i.id")
    List<PackageItemVO> selectItems(@Param("packageId") Long packageId);

    /** 可绑定的商品：该门店已入库且有结存的商品 */
    @Select("SELECT ss.material_id AS materialId, m.code AS materialCode, m.name AS materialName, "
            + "m.spec AS spec, m.unit AS unit, ss.current_qty AS currentQty "
            + "FROM store_stock ss LEFT JOIN material m ON m.id = ss.material_id "
            + "WHERE ss.store_id = #{storeId} AND ss.current_qty > 0 ORDER BY m.name")
    List<PackageItemVO> selectStockMaterials(@Param("storeId") Long storeId);
}
