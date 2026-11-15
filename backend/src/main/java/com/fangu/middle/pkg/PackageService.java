package com.fangu.middle.pkg;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangu.middle.auth.AuthService;
import com.fangu.middle.auth.CurrentAccount;
import com.fangu.middle.common.BizException;
import com.fangu.middle.common.PageResult;
import com.fangu.middle.org.Store;
import com.fangu.middle.org.StoreService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 门店套餐：创建 90/45 分钟套餐并绑定本店已入库的商品 */
@Service
@RequiredArgsConstructor
public class PackageService {

    private final StorePackageMapper packageMapper;
    private final StorePackageItemMapper itemMapper;
    private final PackageQueryMapper queryMapper;
    private final StoreService storeService;
    private final AuthService authService;

    public PageResult<PackageVO> page(long pageNum, long pageSize, String name, String status) {
        return PageResult.of(queryMapper.selectPackagePage(
                new Page<>(pageNum, pageSize),
                name == null || name.isBlank() ? null : name,
                status == null || status.isBlank() ? null : status));
    }

    public PackageDetailVO detail(Long id) {
        StorePackage pkg = requirePackage(id);
        PackageDetailVO vo = new PackageDetailVO();
        vo.setPackageInfo(pkg);
        vo.setItems(queryMapper.selectItems(id));
        return vo;
    }

    /** 核销时使用：套餐必须存在且启用 */
    public PackageDetailVO requireEnabled(Long id) {
        PackageDetailVO vo = detail(id);
        if (!"ENABLED".equals(vo.getPackageInfo().getStatus())) {
            throw new BizException(40004, "套餐已停用，不能核销");
        }
        return vo;
    }

    /** 可绑定商品：本店已入库且有结存的商品 */
    public List<PackageItemVO> materialOptions(Long storeId) {
        return queryMapper.selectStockMaterials(resolveStoreId(storeId));
    }

    @Transactional
    public Long create(PackageReq req) {
        Long storeId = resolveStoreId(req.getStoreId());
        Store store = storeService.getById(storeId);
        if (store == null) {
            throw new BizException(40001, "门店不存在或无权访问");
        }
        validate(req);
        Map<Long, ItemDef> items = normalizeItems(req);

        StorePackage pkg = new StorePackage();
        pkg.setStoreId(store.getId());
        pkg.setFranchiseeId(store.getFranchiseeId());
        pkg.setPackageName(req.getPackageName().trim());
        pkg.setDurationMinutes(req.getDurationMinutes());
        pkg.setPrice(req.getPrice() == null ? BigDecimal.ZERO : req.getPrice());
        pkg.setStatus("ENABLED");
        pkg.setRemark(req.getRemark());
        pkg.setCreatedBy(authService.current().getUserId());
        packageMapper.insert(pkg);

        insertItems(pkg, items);
        return pkg.getId();
    }

    @Transactional
    public void update(Long id, PackageReq req) {
        StorePackage pkg = requirePackage(id);
        validate(req);
        Map<Long, ItemDef> items = normalizeItems(req);

        pkg.setPackageName(req.getPackageName().trim());
        pkg.setDurationMinutes(req.getDurationMinutes());
        pkg.setPrice(req.getPrice() == null ? BigDecimal.ZERO : req.getPrice());
        pkg.setRemark(req.getRemark());
        pkg.setUpdatedAt(null);
        packageMapper.updateById(pkg);

        itemMapper.delete(Wrappers.<StorePackageItem>lambdaQuery()
                .eq(StorePackageItem::getPackageId, id));
        insertItems(pkg, items);
    }

    /** 启用/停用套餐 */
    public void changeStatus(Long id, boolean enabled) {
        StorePackage pkg = requirePackage(id);
        pkg.setStatus(enabled ? "ENABLED" : "DISABLED");
        pkg.setUpdatedAt(null);
        packageMapper.updateById(pkg);
    }

    private StorePackage requirePackage(Long id) {
        StorePackage pkg = packageMapper.selectById(id);
        if (pkg == null) {
            throw new BizException(40001, "套餐不存在或无权访问");
        }
        return pkg;
    }

    private void validate(PackageReq req) {
        if (req.getPackageName() == null || req.getPackageName().isBlank()) {
            throw new BizException(40001, "请填写套餐名称");
        }
        if (req.getDurationMinutes() == null || req.getDurationMinutes() <= 0) {
            throw new BizException(40001, "请填写正确的套餐时长（分钟）");
        }
        if (req.getPrice() != null && req.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(40001, "套餐价格不能为负数");
        }
    }

    /** 合并重复商品，并校验商品必须是本店已入库的商品 */
    private Map<Long, ItemDef> normalizeItems(PackageReq req) {
        Map<Long, ItemDef> merged = new LinkedHashMap<>();
        if (req.getItems() == null) {
            return merged;
        }
        Map<Long, PackageItemVO> stocked = new LinkedHashMap<>();
        for (PackageItemVO vo : queryMapper.selectStockMaterials(resolveStoreId(req.getStoreId()))) {
            stocked.put(vo.getMaterialId(), vo);
        }
        for (ItemReq item : req.getItems()) {
            if (item.getMaterialId() == null) {
                throw new BizException(40001, "请选择商品");
            }
            if (item.getQty() == null || item.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(40001, "商品数量需大于 0");
            }
            String itemType = "OPTIONAL".equals(item.getItemType()) ? "OPTIONAL" : "FIXED";
            PackageItemVO vo = stocked.get(item.getMaterialId());
            if (vo == null) {
                throw new BizException(40001, "商品「" + item.getMaterialId() + "」未入库，不能绑定到套餐");
            }
            if (item.getQty().compareTo(vo.getCurrentQty()) > 0) {
                throw new BizException(40001, "「" + vo.getMaterialName() + "」绑定数量不能超过当前结存 "
                        + vo.getCurrentQty().stripTrailingZeros().toPlainString());
            }
            merged.merge(item.getMaterialId(), new ItemDef(item.getQty(), itemType),
                    (a, b) -> new ItemDef(a.qty().add(b.qty()), a.itemType()));
        }
        return merged;
    }

    private void insertItems(StorePackage pkg, Map<Long, ItemDef> items) {
        for (Map.Entry<Long, ItemDef> entry : items.entrySet()) {
            StorePackageItem item = new StorePackageItem();
            item.setPackageId(pkg.getId());
            item.setStoreId(pkg.getStoreId());
            item.setFranchiseeId(pkg.getFranchiseeId());
            item.setMaterialId(entry.getKey());
            item.setQty(entry.getValue().qty());
            item.setItemType(entry.getValue().itemType());
            itemMapper.insert(item);
        }
    }

    private record ItemDef(BigDecimal qty, String itemType) {
    }

    /** 门店账号取自身门店；加盟商账号须显式传门店并校验归属 */
    private Long resolveStoreId(Long storeId) {
        CurrentAccount acc = authService.current();
        if (acc.getStoreId() != null) {
            return acc.getStoreId();
        }
        if (storeId == null) {
            throw new BizException(40001, "请选择门店");
        }
        Store store = storeService.getById(storeId);
        if (store == null || acc.getFranchiseeId() == null
                || !acc.getFranchiseeId().equals(store.getFranchiseeId())) {
            throw new BizException(40001, "门店不存在或无权访问");
        }
        return storeId;
    }

    @Data
    public static class PackageReq {
        private Long storeId;
        private String packageName;
        private Integer durationMinutes;
        private BigDecimal price;
        private String remark;
        private List<ItemReq> items;
    }

    @Data
    public static class ItemReq {
        private Long materialId;
        private BigDecimal qty;
        /** FIXED 固定消耗 / OPTIONAL 可选，默认 FIXED */
        private String itemType;
    }

    @Data
    public static class PackageDetailVO {
        private StorePackage packageInfo;
        private List<PackageItemVO> items;
    }
}
