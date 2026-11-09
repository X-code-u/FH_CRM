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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FranchiseeService {

    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    private final FranchiseeMapper franchiseeMapper;
    private final FranchiseLevelMapper levelMapper;
    private final StoreMapper storeMapper;

    public Franchisee getById(Long id) {
        return franchiseeMapper.selectById(id);
    }

    /** 分页查询（归属条件由数据权限拦截器在 SQL 层追加） */
    public PageResult<FranchiseeVO> page(FranchiseeQuery query) {
        Page<Franchisee> page = franchiseeMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<Franchisee>lambdaQuery()
                        .like(query.getName() != null && !query.getName().isBlank(),
                                Franchisee::getName, query.getName())
                        .eq(query.getStatus() != null && !query.getStatus().isBlank(),
                                Franchisee::getStatus, query.getStatus())
                        .orderByDesc(Franchisee::getId));
        Map<String, FranchiseLevel> levelMap = levelMapper.selectList(null).stream()
                .collect(Collectors.toMap(FranchiseLevel::getLevelCode, Function.identity()));
        PageResult<FranchiseeVO> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setRecords(page.getRecords().stream().map(f -> {
            FranchiseeVO vo = FranchiseeVO.from(f);
            FranchiseLevel level = levelMap.get(f.getLevelCode());
            if (level != null) {
                vo.setLevelName(level.getLevelName());
                vo.setLevelStatus(level.getStatus());
            }
            return vo;
        }).toList());
        return result;
    }

    public void save(FranchiseeSaveReq req) {
        validate(req);
        if (req.getId() == null) {
            // 新增：编码系统生成 F+4位流水，唯一冲突重试一次
            Franchisee f = new Franchisee();
            applyBasic(f, req);
            f.setStatus("NORMAL");
            insertWithCode(f);
            return;
        }
        // 修改：仅基础档案字段；等级走 /level，状态走 /status，编码不可修改
        Franchisee old = franchiseeMapper.selectById(req.getId());
        if (old == null) {
            throw new BizException(40001, "加盟商不存在");
        }
        applyBasic(old, req);
        try {
            franchiseeMapper.updateById(old);
        } catch (DuplicateKeyException e) {
            throw new BizException(40001, "加盟商名称已存在");
        }
    }

    /** 等级变更：仅总部超管，留痕，不回溯历史（规则 R2/R5） */
    @Audit(type = "FRANCHISEE_LEVEL_CHANGE")
    @Transactional
    public void changeLevel(Long id, String levelCode) {
        Franchisee old = franchiseeMapper.selectById(id);
        if (old == null) {
            throw new BizException(40001, "加盟商不存在");
        }
        Long levelOk = levelMapper.selectCount(Wrappers.<FranchiseLevel>lambdaQuery()
                .eq(FranchiseLevel::getLevelCode, levelCode)
                .eq(FranchiseLevel::getStatus, "ENABLED"));
        if (levelOk == 0) {
            throw new BizException(40001, "等级不存在或已停用");
        }
        AuditContext.setOldValue("levelCode=" + old.getLevelCode());
        Franchisee update = new Franchisee();
        update.setId(id);
        update.setLevelCode(levelCode);
        update.setUpdatedAt(java.time.OffsetDateTime.now());
        franchiseeMapper.updateById(update);
    }

    /**
     * 状态机：NORMAL -> FROZEN / TERMINATED；FROZEN -> NORMAL；TERMINATED 不可逆。
     * 条件更新 WHERE status=前置状态 兜底并发；终止时名下门店自动停用。
     */
    @Audit(type = "FRANCHISEE_STATUS_CHANGE")
    @Transactional
    public void changeStatus(Long id, String target) {
        String from = switch (target) {
            case "FROZEN", "TERMINATED" -> "NORMAL";
            case "NORMAL" -> "FROZEN";
            default -> throw new BizException(40004, "状态取值非法");
        };
        Franchisee old = franchiseeMapper.selectById(id);
        if (old == null) {
            throw new BizException(40001, "加盟商不存在");
        }
        AuditContext.setOldValue("status=" + old.getStatus());
        int rows = franchiseeMapper.update(null, new UpdateWrapper<Franchisee>()
                .eq("id", id).eq("status", from)
                .set("status", target).set("updated_at", java.time.OffsetDateTime.now()));
        if (rows == 0) {
            throw new BizException(40004, "当前状态不允许该操作或并发冲突");
        }
        if ("TERMINATED".equals(target)) {
            storeMapper.update(null, new UpdateWrapper<Store>()
                    .eq("franchisee_id", id)
                    .set("is_active", false).set("updated_at", java.time.OffsetDateTime.now()));
        }
    }

    private void validate(FranchiseeSaveReq req) {
        if (req.getName() == null || req.getName().isBlank()
                || req.getName().length() < 2 || req.getName().length() > 64) {
            throw new BizException(40001, "加盟商名称长度须为 2 至 64 字");
        }
        if (req.getContactName() == null || req.getContactName().isBlank()) {
            throw new BizException(40001, "负责人必填");
        }
        if (req.getContactPhone() == null || !req.getContactPhone().matches(PHONE_REGEX)) {
            throw new BizException(40001, "联系电话格式不正确");
        }
        if (req.getSignDate() == null || req.getSignDate().isAfter(LocalDate.now())) {
            throw new BizException(40001, "签约日期必填且不晚于当前日期");
        }
        if (req.getExpireDate() != null && req.getExpireDate().isBefore(req.getSignDate())) {
            throw new BizException(40001, "到期日期不可早于签约日期");
        }
        if (req.getDeposit() != null && req.getDeposit().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(40001, "保证金不可为负");
        }
        if (req.getAddress() != null && req.getAddress().length() > 128) {
            throw new BizException(40001, "详细地址不超过 128 字");
        }
        Long nameExists = franchiseeMapper.selectCount(Wrappers.<Franchisee>lambdaQuery()
                .eq(Franchisee::getName, req.getName())
                .ne(req.getId() != null, Franchisee::getId, req.getId()));
        if (nameExists > 0) {
            throw new BizException(40001, "加盟商名称已存在");
        }
        if (req.getId() == null) {
            // 合作等级必须为启用状态
            Long levelOk = levelMapper.selectCount(Wrappers.<FranchiseLevel>lambdaQuery()
                    .eq(FranchiseLevel::getLevelCode, req.getLevelCode())
                    .eq(FranchiseLevel::getStatus, "ENABLED"));
            if (req.getLevelCode() == null || levelOk == 0) {
                throw new BizException(40001, "合作等级必须为启用状态的等级");
            }
        }
    }

    private void applyBasic(Franchisee f, FranchiseeSaveReq req) {
        f.setName(req.getName());
        f.setContactName(req.getContactName());
        f.setContactPhone(req.getContactPhone());
        f.setRegion(req.getRegion());
        f.setAddress(req.getAddress());
        if (req.getId() == null) {
            f.setLevelCode(req.getLevelCode());
        }
        f.setSignDate(req.getSignDate());
        f.setExpireDate(req.getExpireDate());
        f.setDeposit(req.getDeposit() == null ? BigDecimal.ZERO : req.getDeposit());
        f.setContractUrl(req.getContractUrl());
        f.setRemark(req.getRemark());
        f.setUpdatedAt(java.time.OffsetDateTime.now());
    }

    /** 编码 F+4位流水；唯一冲突重试一次（不在事务内，避免 PG 事务中止后无法重试） */
    private void insertWithCode(Franchisee f) {
        for (int attempt = 0; attempt < 2; attempt++) {
            f.setFranchiseeCode(String.format("F%04d", franchiseeMapper.maxCodeSeq() + 1));
            try {
                franchiseeMapper.insert(f);
                return;
            } catch (DuplicateKeyException e) {
                if (attempt == 1) {
                    throw new BizException(40005, "编码生成并发冲突，请重试");
                }
            }
        }
    }

    @Data
    public static class FranchiseeQuery {
        private long pageNum = 1;
        private long pageSize = 10;
        private String name;
        private String status;
    }

    @Data
    public static class FranchiseeSaveReq {
        private Long id;
        private String name;
        private String contactName;
        private String contactPhone;
        private String region;
        private String address;
        private String levelCode;
        private LocalDate signDate;
        private LocalDate expireDate;
        private BigDecimal deposit;
        private String contractUrl;
        private String remark;
    }

    @Data
    public static class FranchiseeVO {
        private Long id;
        private String franchiseeCode;
        private String name;
        private String contactName;
        private String contactPhone;
        private String region;
        private String address;
        private String levelCode;
        private String levelName;
        private String levelStatus;
        private LocalDate signDate;
        private LocalDate expireDate;
        private BigDecimal deposit;
        private String status;
        private String remark;

        static FranchiseeVO from(Franchisee f) {
            FranchiseeVO vo = new FranchiseeVO();
            vo.id = f.getId();
            vo.franchiseeCode = f.getFranchiseeCode();
            vo.name = f.getName();
            vo.contactName = f.getContactName();
            vo.contactPhone = f.getContactPhone();
            vo.region = f.getRegion();
            vo.address = f.getAddress();
            vo.levelCode = f.getLevelCode();
            vo.signDate = f.getSignDate();
            vo.expireDate = f.getExpireDate();
            vo.deposit = f.getDeposit();
            vo.status = f.getStatus();
            vo.remark = f.getRemark();
            return vo;
        }
    }
}
