package com.fangu.middle.report;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 核销数据大盘 */
@Data
public class DashboardVO {
    /** 核销单数 */
    private Long totalWriteoffs;
    /** 核销业绩（套餐价格快照合计） */
    private BigDecimal totalRevenue;
    /** 扣减商品总数量 */
    private BigDecimal totalQty;
    /** 套餐核销排行 */
    private List<PackageRankVO> packages;
    /** 产品核销排行 */
    private List<ProductRankVO> products;
    /** 美容师排行 */
    private List<BeauticianRankVO> beauticians;

    @Data
    public static class PackageRankVO {
        private String packageName;
        private Long writeoffCount;
        private BigDecimal revenue;
    }

    @Data
    public static class ProductRankVO {
        private Long materialId;
        private String materialName;
        private String imageUrl;
        private String unit;
        private BigDecimal qty;
    }

    @Data
    public static class BeauticianRankVO {
        private String beautician;
        private Long writeoffCount;
        private BigDecimal revenue;
    }
}
