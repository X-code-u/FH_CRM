package com.fangu.middle.material;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangu.middle.audit.Audit;
import com.fangu.middle.audit.AuditContext;
import com.fangu.middle.common.BizException;
import com.fangu.middle.common.PageResult;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialMapper materialMapper;

    public PageResult<Material> page(MaterialQuery query) {
        Page<Material> page = materialMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<Material>lambdaQuery()
                        .like(query.getName() != null && !query.getName().isBlank(), Material::getName, query.getName())
                        .eq(query.getCategory() != null && !query.getCategory().isBlank(),
                                Material::getCategory, query.getCategory())
                        .eq(query.getIsActive() != null, Material::getIsActive, query.getIsActive())
                        .orderByDesc(Material::getId));
        return PageResult.of(page);
    }

    /** 门店下单可选商品：仅启用商品 */
    public List<Material> options() {
        return materialMapper.selectList(Wrappers.<Material>lambdaQuery()
                .eq(Material::getIsActive, true)
                .orderByAsc(Material::getCode));
    }

    public Material getById(Long id) {
        return materialMapper.selectById(id);
    }

    public java.util.Map<Long, Material> mapByIds(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Map.of();
        }
        return materialMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Material::getId, m -> m));
    }

    /** 库存扣减：null 库存视为无限，不扣；有限库存用条件更新防超卖 */
    public void deductStock(Long materialId, BigDecimal qty) {
        Material m = materialMapper.selectById(materialId);
        if (m == null) {
            throw new BizException(40001, "商品不存在");
        }
        if (m.getStockQty() == null) {
            return;
        }
        if (materialMapper.deductStock(materialId, qty) == 0) {
            throw new BizException(40001, "商品「" + m.getName() + "」库存不足，无法通过审批");
        }
    }

    @Audit(type = "MATERIAL_SAVE")
    public void save(MaterialSaveReq req) {
        validate(req);
        if (req.getId() == null) {
            Material m = new Material();
            apply(m, req);
            m.setIsActive(true);
            insertWithCode(m);
            return;
        }
        Material old = materialMapper.selectById(req.getId());
        if (old == null) {
            throw new BizException(40001, "商品不存在");
        }
        AuditContext.setOldValue("name=" + old.getName() + ", stockQty=" + old.getStockQty()
                + ", baseSalePrice=" + old.getBaseSalePrice() + ", directPrice=" + old.getDirectPrice());
        Material update = new Material();
        update.setId(old.getId());
        apply(update, req);
        update.setIsActive(req.getIsActive() == null ? old.getIsActive() : req.getIsActive());
        update.setUpdatedAt(OffsetDateTime.now());
        materialMapper.updateById(update);
    }

    @Audit(type = "MATERIAL_STATUS_CHANGE")
    public void changeStatus(Long id, boolean active) {
        Material old = materialMapper.selectById(id);
        if (old == null) {
            throw new BizException(40001, "商品不存在");
        }
        AuditContext.setOldValue("isActive=" + old.getIsActive());
        materialMapper.update(null, Wrappers.<Material>lambdaUpdate()
                .eq(Material::getId, id)
                .set(Material::getIsActive, active)
                .set(Material::getUpdatedAt, OffsetDateTime.now()));
    }

    private void validate(MaterialSaveReq req) {
        if (req.getName() == null || req.getName().isBlank() || req.getName().length() > 64) {
            throw new BizException(40001, "商品名称必填且不超过 64 字");
        }
        if (req.getUnit() == null || req.getUnit().isBlank() || req.getUnit().length() > 16) {
            throw new BizException(40001, "计量单位必填且不超过 16 字");
        }
        checkPrice(req.getBaseSalePrice(), "基准售价");
        checkPrice(req.getDirectPrice(), "直营价");
        checkPrice(req.getCostPrice(), "成本价");
        if (req.getStockQty() != null && req.getStockQty().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(40001, "库存不可为负");
        }
        if (req.getWarnMinQty() != null && req.getWarnMinQty().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(40001, "预警值不可为负");
        }
        if (req.getBaseSalePrice() == null || req.getBaseSalePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(40001, "基准售价必须大于 0（否则门店无法下单）");
        }
    }

    private void checkPrice(BigDecimal price, String label) {
        if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(40001, label + "不可为负");
        }
    }

    private void apply(Material m, MaterialSaveReq req) {
        m.setName(req.getName());
        m.setSpec(req.getSpec());
        m.setUnit(req.getUnit());
        m.setCategory(req.getCategory());
        m.setBaseSalePrice(req.getBaseSalePrice());
        m.setDirectPrice(req.getDirectPrice() == null ? BigDecimal.ZERO : req.getDirectPrice());
        m.setCostPrice(req.getCostPrice() == null ? BigDecimal.ZERO : req.getCostPrice());
        m.setWarnMinQty(req.getWarnMinQty() == null ? BigDecimal.ZERO : req.getWarnMinQty());
        // null 表示无限库存
        m.setStockQty(req.getStockQty());
        m.setUpdatedAt(OffsetDateTime.now());
    }

    /** 编码 M+4位流水；唯一冲突重试一次（不在事务内，避免 PG 事务中止后无法重试） */
    private void insertWithCode(Material m) {
        for (int attempt = 0; attempt < 2; attempt++) {
            m.setCode(String.format("M%04d", materialMapper.maxCodeSeq() + 1));
            try {
                materialMapper.insert(m);
                return;
            } catch (org.springframework.dao.DuplicateKeyException e) {
                if (attempt == 1) {
                    throw new BizException(40005, "编码生成并发冲突，请重试");
                }
            }
        }
    }

    @Data
    public static class MaterialQuery {
        private long pageNum = 1;
        private long pageSize = 10;
        private String name;
        private String category;
        private Boolean isActive;
    }

    @Data
    public static class MaterialSaveReq {
        private Long id;
        private String name;
        private String spec;
        private String unit;
        private String category;
        private BigDecimal baseSalePrice;
        private BigDecimal directPrice;
        private BigDecimal costPrice;
        private BigDecimal warnMinQty;
        /** null = 无限库存 */
        private BigDecimal stockQty;
        private Boolean isActive;
    }
}
