package com.fangu.middle.org;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.fangu.middle.common.PageResult;
import com.fangu.middle.common.R;
import com.fangu.middle.datascope.DataScope;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/franchisee")
@RequiredArgsConstructor
public class FranchiseeController {

    private final FranchiseeService franchiseeService;

    /** 分页查询：总部 / 加盟商（加盟商仅能看自身，SQL 层追加归属条件） */
    @SaCheckRole(value = {"HQ_ADMIN", "HQ_OPS", "HQ_FIN", "FRANCHISEE_ADMIN"}, mode = SaMode.OR)
    @DataScope(scope = DataScope.ScopeType.FRANCHISEE)
    @GetMapping("/page")
    public R<PageResult<FranchiseeService.FranchiseeVO>> page(FranchiseeService.FranchiseeQuery query) {
        return R.ok(franchiseeService.page(query));
    }

    /** 新增或修改档案：总部超管 / 运营 */
    @SaCheckRole(value = {"HQ_ADMIN", "HQ_OPS"}, mode = SaMode.OR)
    @PostMapping("/save")
    public R<Void> save(@RequestBody FranchiseeService.FranchiseeSaveReq req) {
        franchiseeService.save(req);
        return R.ok(null);
    }

    /** 变更等级：仅总部超管 */
    @SaCheckRole("HQ_ADMIN")
    @PutMapping("/{id}/level")
    public R<Void> changeLevel(@PathVariable Long id, @RequestBody LevelReq req) {
        franchiseeService.changeLevel(id, req.getLevelCode());
        return R.ok(null);
    }

    /** 状态流转：总部超管 / 运营；NORMAL->FROZEN/TERMINATED，FROZEN->NORMAL */
    @SaCheckRole(value = {"HQ_ADMIN", "HQ_OPS"}, mode = SaMode.OR)
    @PutMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @RequestBody StatusReq req) {
        franchiseeService.changeStatus(id, req.getStatus());
        return R.ok(null);
    }

    @Data
    public static class LevelReq {
        private String levelCode;
    }

    @Data
    public static class StatusReq {
        private String status;
    }
}
