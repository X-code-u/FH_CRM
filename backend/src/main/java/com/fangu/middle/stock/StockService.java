package com.fangu.middle.stock;

import com.fangu.middle.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StoreStockMapper storeStockMapper;
    private final StockFlowMapper stockFlowMapper;

    /**
     * 采购入库：按门店 + 物料累加结存并写流水。
     * requestNo 为幂等号，重复提交直接返回。
     */
    @Transactional
    public void inbound(Long storeId, Long orderId, List<InboundItem> items, String requestNo) {
        if (stockFlowMapper.countByRequestNo(requestNo) > 0) {
            return;
        }
        for (InboundItem item : items) {
            if (storeStockMapper.addQty(storeId, item.materialId(), item.qty()) == 0) {
                try {
                    storeStockMapper.insertRow(storeId, item.materialId(), item.qty());
                } catch (DuplicateKeyException e) {
                    // 并发首次入库：唯一键冲突后重试一次自增
                    storeStockMapper.addQty(storeId, item.materialId(), item.qty());
                }
            }
            StockFlow flow = new StockFlow();
            flow.setStoreId(storeId);
            flow.setMaterialId(item.materialId());
            flow.setFlowType("SALE_IN");
            flow.setRelateOrderId(orderId);
            flow.setRequestNo(requestNo);
            flow.setQty(item.qty());
            stockFlowMapper.insert(flow);
        }
    }

    public record InboundItem(Long materialId, BigDecimal qty) {
    }

    /**
     * 套餐核销出库：按门店 + 物料原子扣减结存并写流水。
     * requestNo 为幂等号，重复提交直接返回；结存不足抛业务异常（整体回滚）。
     */
    @Transactional
    public void outbound(Long storeId, Long writeoffId, List<OutboundItem> items, String requestNo) {
        if (stockFlowMapper.countByRequestNo(requestNo) > 0) {
            return;
        }
        for (OutboundItem item : items) {
            if (storeStockMapper.deductQty(storeId, item.materialId(), item.qty()) == 0) {
                throw new BizException(40001, "「" + item.materialName() + "」库存不足，无法核销");
            }
            StockFlow flow = new StockFlow();
            flow.setStoreId(storeId);
            flow.setMaterialId(item.materialId());
            flow.setFlowType("WRITE_OFF");
            flow.setRelateOrderId(writeoffId);
            flow.setRequestNo(requestNo);
            flow.setQty(item.qty().negate());
            stockFlowMapper.insert(flow);
        }
    }

    public record OutboundItem(Long materialId, BigDecimal qty, String materialName) {
    }
}
