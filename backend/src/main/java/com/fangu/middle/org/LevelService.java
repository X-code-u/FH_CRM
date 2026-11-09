package com.fangu.middle.org;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fangu.middle.audit.Audit;
import com.fangu.middle.audit.AuditContext;
import com.fangu.middle.common.BizException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LevelService {

    private final FranchiseLevelMapper levelMapper;

    public List<FranchiseLevel> list() {
        return levelMapper.selectList(Wrappers.<FranchiseLevel>lambdaQuery()
                .orderByAsc(FranchiseLevel::getSortNo).orderByAsc(FranchiseLevel::getId));
    }

    @Audit(type = "LEVEL_SAVE")
    public void save(LevelSaveReq req) {
        if (req.getLevelName() == null || req.getLevelName().isBlank() || req.getLevelName().length() > 32) {
            throw new BizException(40001, "等级名称必填且不超过 32 字");
        }
        if (req.getStatus() != null && !List.of("ENABLED", "DISABLED").contains(req.getStatus())) {
            throw new BizException(40001, "状态取值非法");
        }
        if (req.getId() == null) {
            // 新增：编码唯一，仅允许大写字母与下划线，长度不超过 16 位
            if (req.getLevelCode() == null || !req.getLevelCode().matches("^[A-Z_]{1,16}$")) {
                throw new BizException(40001, "等级编码仅允许大写字母与下划线，长度不超过 16 位");
            }
            Long exists = levelMapper.selectCount(Wrappers.<FranchiseLevel>lambdaQuery()
                    .eq(FranchiseLevel::getLevelCode, req.getLevelCode()));
            if (exists > 0) {
                throw new BizException(40001, "等级编码已存在");
            }
            FranchiseLevel level = new FranchiseLevel();
            level.setLevelCode(req.getLevelCode());
            level.setLevelName(req.getLevelName());
            level.setSortNo(req.getSortNo() == null ? 0 : req.getSortNo());
            level.setStatus(req.getStatus() == null ? "ENABLED" : req.getStatus());
            level.setRemark(req.getRemark());
            levelMapper.insert(level);
            return;
        }
        // 修改：编码一经创建不可修改（定价关联键）
        FranchiseLevel old = levelMapper.selectById(req.getId());
        if (old == null) {
            throw new BizException(40001, "等级不存在");
        }
        AuditContext.setOldValue("levelCode=" + old.getLevelCode() + ", levelName=" + old.getLevelName()
                + ", sortNo=" + old.getSortNo() + ", status=" + old.getStatus());
        FranchiseLevel update = new FranchiseLevel();
        update.setId(old.getId());
        update.setLevelName(req.getLevelName());
        update.setSortNo(req.getSortNo());
        update.setStatus(req.getStatus());
        update.setRemark(req.getRemark());
        levelMapper.updateById(update);
    }

    @Data
    public static class LevelSaveReq {
        private Long id;
        private String levelCode;
        private String levelName;
        private Integer sortNo;
        private String status;
        private String remark;
    }
}
