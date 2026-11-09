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
@RequestMapping("/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    /** 分页查询：总部 / 加盟商（加盟商仅能看名下门店，SQL 层追加归属条件） */
    @SaCheckRole(value = {"HQ_ADMIN", "HQ_OPS", "HQ_FIN", "FRANCHISEE_ADMIN"}, mode = SaMode.OR)
    @DataScope(scope = DataScope.ScopeType.FRANCHISEE)
    @GetMapping("/page")
    public R<PageResult<StoreService.StoreVO>> page(StoreService.StoreQuery query) {
        return R.ok(storeService.page(query));
    }

    /** 新增或修改门店：总部超管 / 运营 */
    @SaCheckRole(value = {"HQ_ADMIN", "HQ_OPS"}, mode = SaMode.OR)
    @PostMapping("/save")
    public R<Void> save(@RequestBody StoreService.StoreSaveReq req) {
        storeService.save(req);
        return R.ok(null);
    }

    /** 启用 / 停用：总部超管 / 运营 */
    @SaCheckRole(value = {"HQ_ADMIN", "HQ_OPS"}, mode = SaMode.OR)
    @PutMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @RequestBody StatusReq req) {
        storeService.changeStatus(id, req.isActive());
        return R.ok(null);
    }

    @Data
    public static class StatusReq {
        private boolean active;
    }
}
