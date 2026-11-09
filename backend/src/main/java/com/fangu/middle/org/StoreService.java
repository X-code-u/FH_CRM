package com.fangu.middle.org;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangu.middle.audit.Audit;
import com.fangu.middle.audit.AuditContext;
import com.fangu.middle.common.BizException;
import com.fangu.middle.common.PageResult;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreService {

    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    private final StoreMapper storeMapper;
    private final FranchiseeMapper franchiseeMapper;

    public Store getById(Long id) {
        return storeMapper.selectById(id);
    }

    /** 分页查询（归属条件由数据权限拦截器在 SQL 层追加） */
    public PageResult<StoreVO> page(StoreQuery query) {
        Page<Store> page = storeMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<Store>lambdaQuery()
                        .like(query.getName() != null && !query.getName().isBlank(),
                                Store::getName, query.getName())
                        .eq(query.getStoreType() != null && !query.getStoreType().isBlank(),
                                Store::getStoreType, query.getStoreType())
                        .eq(query.getFranchiseeId() != null, Store::getFranchiseeId, query.getFranchiseeId())
                        .orderByDesc(Store::getId));
        List<Store> records = page.getRecords();
        Map<Long, String> franchiseeNames = records.isEmpty() ? Map.of()
                : franchiseeMapper.selectBatchIds(records.stream()
                        .map(Store::getFranchiseeId).filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Franchisee::getId, Franchisee::getName));
        PageResult<StoreVO> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setRecords(records.stream().map(s -> {
            StoreVO vo = StoreVO.from(s);
            vo.setFranchiseeName(s.getFranchiseeId() == null ? null : franchiseeNames.get(s.getFranchiseeId()));
            return vo;
        }).toList());
        return result;
    }

    public void save(StoreSaveReq req) {
        if (req.getId() == null) {
            validate(req, true, req.getStoreType(), req.getFranchiseeId());
            Store s = new Store();
            applyBasic(s, req);
            s.setStoreType(req.getStoreType());
            s.setFranchiseeId("JOIN".equals(req.getStoreType()) ? req.getFranchiseeId() : null);
            s.setIsActive(true);
            insertWithCode(s);
            return;
        }
        // 修改：门店类型创建后不可修改，归属加盟商不可变更；名称唯一性按既有类型与归属校验
        Store old = storeMapper.selectById(req.getId());
        if (old == null) {
            throw new BizException(40001, "门店不存在");
        }
        if (req.getStoreType() != null && !req.getStoreType().equals(old.getStoreType())) {
            throw new BizException(40001, "门店类型创建后不可修改");
        }
        validate(req, false, old.getStoreType(), old.getFranchiseeId());
        applyBasic(old, req);
        try {
            storeMapper.updateById(old);
        } catch (DuplicateKeyException e) {
            throw new BizException(40001, "同一加盟商下门店名称不可重复");
        }
    }

    @Audit(type = "STORE_STATUS_CHANGE")
    public void changeStatus(Long id, boolean active) {
        Store old = storeMapper.selectById(id);
        if (old == null) {
            throw new BizException(40001, "门店不存在");
        }
        AuditContext.setOldValue("isActive=" + old.getIsActive());
        int rows = storeMapper.update(null, new UpdateWrapper<Store>()
                .eq("id", id).eq("is_active", !active)
                .set("is_active", active).set("updated_at", OffsetDateTime.now()));
        if (rows == 0) {
            throw new BizException(40004, "门店已处于目标状态或并发冲突");
        }
    }

    private void validate(StoreSaveReq req, boolean create, String storeType, Long franchiseeId) {
        if (create && !List.of("JOIN", "DIRECT").contains(storeType == null ? "" : storeType)) {
            throw new BizException(40001, "门店类型必填，取值为 JOIN 加盟 / DIRECT 直营");
        }
        if (create && "JOIN".equals(storeType)) {
            if (franchiseeId == null) {
                throw new BizException(40001, "加盟门店必须选择所属加盟商");
            }
            Franchisee f = franchiseeMapper.selectById(franchiseeId);
            if (f == null || !"NORMAL".equals(f.getStatus())) {
                throw new BizException(40001, "所属加盟商不存在或非正常状态");
            }
        }
        if (create && "DIRECT".equals(storeType) && franchiseeId != null) {
            throw new BizException(40001, "直营门店所属加盟商必须为空");
        }
        if (req.getName() == null || req.getName().isBlank() || req.getName().length() > 64) {
            throw new BizException(40001, "门店名称必填且不超过 64 字");
        }
        if (req.getAddress() == null || req.getAddress().isBlank() || req.getAddress().length() > 128) {
            throw new BizException(40001, "详细地址必填且不超过 128 字");
        }
        if (req.getContactPhone() != null && !req.getContactPhone().isBlank()
                && !req.getContactPhone().matches(PHONE_REGEX)) {
            throw new BizException(40001, "联系电话格式不正确");
        }
        if (req.getOpenDate() != null && req.getOpenDate().isAfter(LocalDate.now())) {
            throw new BizException(40001, "开业日期不晚于当前日期");
        }
        // 名称唯一：加盟门店同一加盟商内唯一；直营门店全局唯一
        Long nameExists = storeMapper.selectCount(Wrappers.<Store>lambdaQuery()
                .eq(Store::getName, req.getName())
                .ne(req.getId() != null, Store::getId, req.getId())
                .and(w -> {
                    if ("DIRECT".equals(storeType)) {
                        w.isNull(Store::getFranchiseeId);
                    } else {
                        w.eq(Store::getFranchiseeId, franchiseeId);
                    }
                }));
        if (nameExists > 0) {
            throw new BizException(40001, "DIRECT".equals(storeType)
                    ? "直营门店名称全局唯一，已存在同名门店" : "同一加盟商下门店名称不可重复");
        }
    }

    private void applyBasic(Store s, StoreSaveReq req) {
        s.setName(req.getName());
        s.setAddress(req.getAddress());
        s.setContactName(req.getContactName());
        s.setContactPhone(req.getContactPhone());
        s.setOpenDate(req.getOpenDate());
        s.setUpdatedAt(OffsetDateTime.now());
    }

    /** 编码 S+4位流水；唯一冲突重试一次（不在事务内，避免 PG 事务中止后无法重试） */
    private void insertWithCode(Store s) {
        for (int attempt = 0; attempt < 2; attempt++) {
            s.setStoreCode(String.format("S%04d", storeMapper.maxCodeSeq() + 1));
            try {
                storeMapper.insert(s);
                return;
            } catch (DuplicateKeyException e) {
                if (attempt == 1) {
                    throw new BizException(40005, "编码生成并发冲突，请重试");
                }
            }
        }
    }

    @Data
    public static class StoreQuery {
        private long pageNum = 1;
        private long pageSize = 10;
        private String name;
        private String storeType;
        private Long franchiseeId;
    }

    @Data
    public static class StoreSaveReq {
        private Long id;
        private String name;
        private String storeType;
        private Long franchiseeId;
        private String address;
        private String contactName;
        private String contactPhone;
        private LocalDate openDate;
    }

    @Data
    public static class StoreVO {
        private Long id;
        private String storeCode;
        private String name;
        private String storeType;
        private Long franchiseeId;
        private String franchiseeName;
        private String address;
        private String contactName;
        private String contactPhone;
        private LocalDate openDate;
        private Boolean isActive;

        static StoreVO from(Store s) {
            StoreVO vo = new StoreVO();
            vo.id = s.getId();
            vo.storeCode = s.getStoreCode();
            vo.name = s.getName();
            vo.storeType = s.getStoreType();
            vo.franchiseeId = s.getFranchiseeId();
            vo.address = s.getAddress();
            vo.contactName = s.getContactName();
            vo.contactPhone = s.getContactPhone();
            vo.openDate = s.getOpenDate();
            vo.isActive = s.getIsActive();
            return vo;
        }
    }
}
