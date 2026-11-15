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
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(order_no, 11) AS SIGNED)), 0) FROM sale_order "
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

    /** 付款：仅审核通过且未付款的订单可置为付款成功 */
    @Update("UPDATE sale_order SET pay_status = 'PAID', paid_at = now(), updated_at = now() "
            + "WHERE id = #{id} AND status = 'CONFIRMED' AND pay_status = 'UNPAID'")
    int payIfUnpaid(@Param("id") Long id);

    /** 总部确认收款：门店已付款且尚未确认收款 */
    @Update("UPDATE sale_order SET receipt_status = 'CONFIRMED', receipt_at = now(), updated_at = now() "
            + "WHERE id = #{id} AND status = 'CONFIRMED' AND pay_status = 'PAID' "
            + "AND receipt_status = 'UNCONFIRMED'")
    int confirmReceiptIfPaid(@Param("id") Long id);

    /** 发货：已收款且未发货的订单可置为已发货 */
    @Update("UPDATE sale_order SET status = 'SHIPPED', shipped_at = now(), updated_at = now() "
            + "WHERE id = #{id} AND status = 'CONFIRMED' AND pay_status = 'PAID' "
            + "AND receipt_status = 'CONFIRMED'")
    int shipIfReady(@Param("id") Long id);

    /** 门店确认入库：仅已发货订单可入库 */
    @Update("UPDATE sale_order SET status = 'RECEIVED', received_at = now(), updated_at = now() "
            + "WHERE id = #{id} AND status = 'SHIPPED'")
    int receiveIfShipped(@Param("id") Long id);

    /** 审批改量时同步明细数量与金额（价格快照不变） */
    @Update("UPDATE sale_order_item SET qty = #{qty}, amount = #{amount} "
            + "WHERE id = #{itemId} AND order_id = #{orderId}")
    int updateItemQty(@Param("orderId") Long orderId, @Param("itemId") Long itemId,
                      @Param("qty") BigDecimal qty, @Param("amount") BigDecimal amount);
}
