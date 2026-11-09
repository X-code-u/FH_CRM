package com.fangu.middle.sale;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangu.middle.auth.AuthService;
import com.fangu.middle.auth.CurrentAccount;
import com.fangu.middle.common.BizException;
import com.fangu.middle.common.PageResult;
import com.fangu.middle.material.Material;
import com.fangu.middle.material.MaterialService;
import com.fangu.middle.org.Franchisee;
import com.fangu.middle.org.FranchiseeService;
import com.fangu.middle.org.Store;
import com.fangu.middle.org.StoreService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 门店采购单：门店下单 -> 总部审批（可改数量）-> 通过 / 驳回。
 * 状态映射沿用单据枚举：PENDING 待审批、CONFIRMED 已通过、CANCELLED 已驳回。
 */
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private static final DateTimeFormatter DATE_PART = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper itemMapper;
    private final MaterialService materialService;
    private final StoreService storeService;
    private final FranchiseeService franchiseeService;
    private final PricingResolver pricingResolver;
    private final AuthService authService;

    /** 门店下单 */
    @Transactional
    public String create(CreateReq req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BizException(40001, "请至少选择一个商品");
        }
        if (req.getStoreId() == null) {
            throw new BizException(40001, "请选择门店");
        }
        Store store = requireOrderableStore(req.getStoreId());
        Franchisee franchisee = null;
        if ("JOIN".equals(store.getStoreType())) {
            franchisee = franchiseeService.getById(store.getFranchiseeId());
            if (franchisee == null || !"NORMAL".equals(franchisee.getStatus())) {
                throw new BizException(40002, "所属加盟商非正常状态，门店不可用");
            }
        }

        // 合并同商品重复行，避免同一商品拆成多行导致库存校验失真
        Map<Long, BigDecimal> mergedQty = new LinkedHashMap<>();
        for (ItemReq item : req.getItems()) {
            if (item.getMaterialId() == null || item.getQty() == null
                    || item.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(40001, "商品与数量必须填写，数量需大于 0");
            }
            mergedQty.merge(item.getMaterialId(), item.getQty(), BigDecimal::add);
        }

        BigDecimal total = BigDecimal.ZERO;
        List<PurchaseOrderItem> items = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : mergedQty.entrySet()) {
            Material material = materialService.getById(entry.getKey());
            if (material == null || !Boolean.TRUE.equals(material.getIsActive())) {
                throw new BizException(40001, "商品不存在或已停用");
            }
            PricingResolver.PriceResult pricing = pricingResolver.resolve(
                    material, store.getStoreType(),
                    franchisee == null ? null : franchisee.getLevelCode());
            pricingResolver.ensurePositive(pricing.price(), material.getName());
            ensureStockEnough(material, entry.getValue());

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setMaterialId(material.getId());
            item.setQty(entry.getValue());
            item.setPrice(pricing.price());
            item.setAmount(money(entry.getValue().multiply(pricing.price())));
            item.setPriceSource(pricing.source());
            items.add(item);
            total = total.add(item.getAmount());
        }

        PurchaseOrder order = new PurchaseOrder();
        order.setStoreId(store.getId());
        order.setStoreTypeSnapshot(store.getStoreType());
        order.setFranchiseeIdSnapshot(store.getFranchiseeId());
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(money(total));
        order.setStatus("PENDING");
        order.setRemark(req.getRemark());
        order.setCreatedBy(authService.current().getUserId());
        insertWithOrderNo(order);

        for (PurchaseOrderItem item : items) {
            item.setOrderId(order.getId());
            itemMapper.insert(item);
        }
        return order.getOrderNo();
    }

    public PageResult<OrderVO> page(OrderQuery query) {
        Page<PurchaseOrder> page = orderMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<PurchaseOrder>lambdaQuery()
                        .eq(query.getStatus() != null && !query.getStatus().isBlank(),
                                PurchaseOrder::getStatus, query.getStatus())
                        .like(query.getOrderNo() != null && !query.getOrderNo().isBlank(),
                                PurchaseOrder::getOrderNo, query.getOrderNo())
                        .eq(query.getStoreId() != null, PurchaseOrder::getStoreId, query.getStoreId())
                        .orderByDesc(PurchaseOrder::getId));
        Map<Long, Store> stores = storeMap(page.getRecords().stream()
                .map(PurchaseOrder::getStoreId).filter(Objects::nonNull).distinct().toList());
        PageResult<OrderVO> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setRecords(page.getRecords().stream().map(o -> {
            OrderVO vo = OrderVO.from(o);
            Store store = stores.get(o.getStoreId());
            if (store != null) {
                vo.setStoreCode(store.getStoreCode());
                vo.setStoreName(store.getName());
                vo.setStoreType(store.getStoreType());
            }
            return vo;
        }).toList());
        return result;
    }

    public OrderDetailVO detail(Long id) {
        PurchaseOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(40001, "采购单不存在或无权访问");
        }
        List<PurchaseOrderItem> items = itemMapper.selectList(Wrappers.<PurchaseOrderItem>lambdaQuery()
                .eq(PurchaseOrderItem::getOrderId, id).orderByAsc(PurchaseOrderItem::getId));
        Map<Long, Material> materials = materialService.mapByIds(
                items.stream().map(PurchaseOrderItem::getMaterialId).distinct().toList());
        Store store = storeService.getById(order.getStoreId());

        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrder(OrderVO.from(order));
        if (store != null) {
            vo.getOrder().setStoreCode(store.getStoreCode());
            vo.getOrder().setStoreName(store.getName());
            vo.getOrder().setStoreType(store.getStoreType());
        }
        vo.setItems(items.stream().map(i -> {
            ItemVO iv = ItemVO.from(i);
            Material m = materials.get(i.getMaterialId());
            if (m != null) {
                iv.setMaterialCode(m.getCode());
                iv.setMaterialName(m.getName());
                iv.setUnit(m.getUnit());
                iv.setSpec(m.getSpec());
            }
            return iv;
        }).toList());
        return vo;
    }

    /** 总部审批通过：可修改明细数量，价格快照不变，通过后扣减有限库存 */
    @Transactional
    public void approve(Long id, ApproveReq req) {
        PurchaseOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(40001, "采购单不存在");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BizException(40004, "仅待审批单据可审批");
        }
        List<PurchaseOrderItem> items = itemMapper.selectList(Wrappers.<PurchaseOrderItem>lambdaQuery()
                .eq(PurchaseOrderItem::getOrderId, id));
        Map<Long, PurchaseOrderItem> itemMap = new LinkedHashMap<>();
        items.forEach(i -> itemMap.put(i.getId(), i));

        if (req.getItems() != null) {
            for (QtyReq qtyReq : req.getItems()) {
                PurchaseOrderItem item = itemMap.get(qtyReq.getItemId());
                if (item == null) {
                    throw new BizException(40001, "明细不属于该采购单");
                }
                if (qtyReq.getQty() == null || qtyReq.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BizException(40001, "审批数量需大于 0");
                }
                item.setQty(qtyReq.getQty());
                item.setAmount(money(qtyReq.getQty().multiply(item.getPrice())));
                orderMapper.updateItemQty(id, item.getId(), item.getQty(), item.getAmount());
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItem item : itemMap.values()) {
            materialService.deductStock(item.getMaterialId(), item.getQty());
            total = total.add(item.getAmount());
        }

        if (orderMapper.approveIfPending(id, money(total)) == 0) {
            throw new BizException(40005, "单据状态已变更，请刷新后重试");
        }
    }

    /** 总部驳回：必须填写驳回原因 */
    @Transactional
    public void reject(Long id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BizException(40001, "请填写驳回原因");
        }
        PurchaseOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(40001, "采购单不存在");
        }
        if (orderMapper.rejectIfPending(id, reason) == 0) {
            throw new BizException(40004, "仅待审批单据可驳回");
        }
    }

    /** 门店可用性 + 归属校验：门店账号只能给自己门店下单，加盟商只能给名下门店下单 */
    private Store requireOrderableStore(Long storeId) {
        CurrentAccount acc = authService.current();
        Store store = storeService.getById(storeId);
        if (store == null || !Boolean.TRUE.equals(store.getIsActive())) {
            throw new BizException(40002, "门店不存在或已停用");
        }
        String scope = acc.getDataScope();
        if ("STORE".equals(scope) && !storeId.equals(acc.getStoreId())) {
            throw new BizException(403, "只能为本门店下单");
        }
        if ("FRANCHISEE".equals(scope) && !store.getFranchiseeId().equals(acc.getFranchiseeId())) {
            throw new BizException(403, "只能为名下门店下单");
        }
        if ("ALL".equals(scope)) {
            throw new BizException(403, "采购单由门店发起，总部不支持下单");
        }
        return store;
    }

    /** 下单前置校验：有限库存不足时直接拦截，避免审批时才失败 */
    private void ensureStockEnough(Material material, BigDecimal qty) {
        if (material.getStockQty() != null && material.getStockQty().compareTo(qty) < 0) {
            throw new BizException(40001, "商品「" + material.getName() + "」库存不足，当前可售 "
                    + material.getStockQty().stripTrailingZeros().toPlainString());
        }
    }

    private Map<Long, Store> storeMap(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Store> map = new LinkedHashMap<>();
        for (Long id : ids) {
            Store s = storeService.getById(id);
            if (s != null) {
                map.put(id, s);
            }
        }
        return map;
    }

    private BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    /** 单号 SO + yyyyMMdd + 4 位当日流水，唯一冲突重试一次（规则：单号生成） */
    private void insertWithOrderNo(PurchaseOrder order) {
        String datePart = LocalDate.now().format(DATE_PART);
        for (int attempt = 0; attempt < 2; attempt++) {
            order.setOrderNo("SO" + datePart + String.format("%04d", orderMapper.maxOrderSeq(datePart) + 1));
            try {
                orderMapper.insert(order);
                return;
            } catch (DuplicateKeyException e) {
                if (attempt == 1) {
                    throw new BizException(40005, "单号生成并发冲突，请重试");
                }
            }
        }
    }

    @Data
    public static class CreateReq {
        private Long storeId;
        private String remark;
        private List<ItemReq> items;
    }

    @Data
    public static class ItemReq {
        private Long materialId;
        private BigDecimal qty;
    }

    @Data
    public static class ApproveReq {
        private List<QtyReq> items;
    }

    @Data
    public static class QtyReq {
        private Long itemId;
        private BigDecimal qty;
    }

    @Data
    public static class OrderQuery {
        private long pageNum = 1;
        private long pageSize = 10;
        private String status;
        private String orderNo;
        private Long storeId;
    }

    @Data
    public static class OrderVO {
        private Long id;
        private String orderNo;
        private Long storeId;
        private String storeCode;
        private String storeName;
        private String storeType;
        private LocalDate orderDate;
        private BigDecimal totalAmount;
        private String status;
        private String remark;
        private String cancelReason;
        private OffsetDateTime confirmedAt;
        private OffsetDateTime createdAt;

        static OrderVO from(PurchaseOrder o) {
            OrderVO vo = new OrderVO();
            vo.id = o.getId();
            vo.orderNo = o.getOrderNo();
            vo.storeId = o.getStoreId();
            vo.orderDate = o.getOrderDate();
            vo.totalAmount = o.getTotalAmount();
            vo.status = o.getStatus();
            vo.remark = o.getRemark();
            vo.cancelReason = o.getCancelReason();
            vo.confirmedAt = o.getConfirmedAt();
            vo.createdAt = o.getCreatedAt();
            return vo;
        }
    }

    @Data
    public static class ItemVO {
        private Long id;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String spec;
        private String unit;
        private BigDecimal qty;
        private BigDecimal price;
        private BigDecimal amount;
        private String priceSource;

        static ItemVO from(PurchaseOrderItem i) {
            ItemVO vo = new ItemVO();
            vo.id = i.getId();
            vo.materialId = i.getMaterialId();
            vo.qty = i.getQty();
            vo.price = i.getPrice();
            vo.amount = i.getAmount();
            vo.priceSource = i.getPriceSource();
            return vo;
        }
    }

    @Data
    public static class OrderDetailVO {
        private OrderVO order;
        private List<ItemVO> items;
    }
}
