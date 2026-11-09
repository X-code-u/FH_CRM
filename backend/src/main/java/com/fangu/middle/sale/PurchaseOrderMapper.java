package com.fangu.middle.sale;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {

    /** 当日单号流水最大值（单号格式 SO + yyyyMMdd + 4 位流水） */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(order_no, 11) AS INTEGER)), 0) FROM sale_order "
            + "WHERE order_no LIKE CONCAT('SO', #{datePart}, '%')")
    int maxOrderSeq(@Param("datePart") String datePart);

    /** 审批通过：条件更新兜底并发，仅 PENDING 可流转 */
    @Update("UPDATE sale_order SET status = 'CONFIRMED', total_amount = #{totalAmount}, "
            + "confirmed_at = now(), updated_at = now() WHERE id = #{id} AND status = 'PENDING'")
    int approveIfPending(@Param("id") Long id, @Param("totalAmount") BigDecimal totalAmount);

    /** 驳回：条件更新兜底并发，仅 PENDING 可流转 */
    @Update("UPDATE sale_order SET status = 'CANCELLED', cancel_reason = #{reason}, "
            + "updated_at = now() WHERE id = #{id} AND status = 'PENDING'")
    int rejectIfPending(@Param("id") Long id, @Param("reason") String reason);

    /** 审批改量时同步明细数量与金额（价格快照不变） */
    @Update("UPDATE sale_order_item SET qty = #{qty}, amount = #{amount} "
            + "WHERE id = #{itemId} AND order_id = #{orderId}")
    int updateItemQty(@Param("orderId") Long orderId, @Param("itemId") Long itemId,
                      @Param("qty") BigDecimal qty, @Param("amount") BigDecimal amount);
}
