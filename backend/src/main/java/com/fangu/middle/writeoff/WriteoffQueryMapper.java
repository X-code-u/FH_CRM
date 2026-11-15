package com.fangu.middle.writeoff;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WriteoffQueryMapper {

    /** 核销记录：主表 store_writeoff，数据权限拦截器按 store_id / franchisee_id 追加条件 */
    @Select("<script>"
            + "SELECT w.id, w.writeoff_no AS writeoffNo, w.package_id AS packageId, w.package_name AS packageName, "
            + "w.store_id AS storeId, s.name AS storeName, w.beautician AS beautician, "
            + "w.package_price AS packagePrice, w.remark, w.created_at AS createdAt, "
            + "(SELECT COUNT(*) FROM store_writeoff_item i WHERE i.writeoff_id = w.id) AS itemCount, "
            + "(SELECT COALESCE(SUM(i.qty), 0) FROM store_writeoff_item i WHERE i.writeoff_id = w.id) AS totalQty "
            + "FROM store_writeoff w LEFT JOIN store s ON s.id = w.store_id "
            + "WHERE 1 = 1 "
            + "<if test='packageName != null and packageName != \"\"'>AND w.package_name LIKE CONCAT('%', #{packageName}, '%') </if>"
            + "ORDER BY w.id DESC"
            + "</script>")
    IPage<WriteoffVO> selectPage(IPage<WriteoffVO> page, @Param("packageName") String packageName);

    /** 核销明细 */
    @Select("SELECT i.material_id AS materialId, m.code AS materialCode, m.name AS materialName, "
            + "m.spec AS spec, m.unit AS unit, i.qty AS qty, i.item_type AS itemType "
            + "FROM store_writeoff_item i LEFT JOIN material m ON m.id = i.material_id "
            + "WHERE i.writeoff_id = #{writeoffId} ORDER BY i.id")
    List<WriteoffItemVO> selectItems(@Param("writeoffId") Long writeoffId);
}
