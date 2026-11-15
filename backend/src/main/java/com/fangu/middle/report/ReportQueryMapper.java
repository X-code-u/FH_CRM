package com.fangu.middle.report;

import com.fangu.middle.report.DashboardVO.BeauticianRankVO;
import com.fangu.middle.report.DashboardVO.PackageRankVO;
import com.fangu.middle.report.DashboardVO.ProductRankVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ReportQueryMapper {

    @Select("SELECT COUNT(*) FROM store_writeoff "
            + "WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    long countWriteoffs(@Param("days") int days);

    @Select("SELECT COALESCE(SUM(package_price), 0) FROM store_writeoff "
            + "WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    BigDecimal sumRevenue(@Param("days") int days);

    @Select("SELECT COALESCE(SUM(qty), 0) FROM store_writeoff_item "
            + "WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    BigDecimal sumQty(@Param("days") int days);

    /** 套餐核销排行 */
    @Select("SELECT package_name AS packageName, COUNT(*) AS writeoffCount, "
            + "COALESCE(SUM(package_price), 0) AS revenue FROM store_writeoff "
            + "WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) "
            + "GROUP BY package_name ORDER BY writeoffCount DESC, revenue DESC LIMIT 10")
    List<PackageRankVO> packageRank(@Param("days") int days);

    /** 产品核销排行（带商品图） */
    @Select("SELECT i.material_id AS materialId, m.name AS materialName, m.image_url AS imageUrl, "
            + "m.unit AS unit, SUM(i.qty) AS qty FROM store_writeoff_item i "
            + "LEFT JOIN material m ON m.id = i.material_id "
            + "WHERE i.created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) "
            + "GROUP BY i.material_id, m.name, m.image_url, m.unit "
            + "ORDER BY qty DESC LIMIT 10")
    List<ProductRankVO> productRank(@Param("days") int days);

    /** 美容师排行（按核销单数，业绩取套餐价格快照） */
    @Select("SELECT beautician, COUNT(*) AS writeoffCount, "
            + "COALESCE(SUM(package_price), 0) AS revenue FROM store_writeoff "
            + "WHERE created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) "
            + "AND beautician IS NOT NULL AND beautician <> '' "
            + "GROUP BY beautician ORDER BY writeoffCount DESC, revenue DESC LIMIT 10")
    List<BeauticianRankVO> beauticianRank(@Param("days") int days);
}
