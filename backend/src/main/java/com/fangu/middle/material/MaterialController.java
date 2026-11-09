package com.fangu.middle.material;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.fangu.middle.common.PageResult;
import com.fangu.middle.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/material")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    /** 商品分页：总部维护 */
    @SaCheckRole(value = {"HQ_ADMIN", "HQ_OPS", "HQ_FIN"}, mode = SaMode.OR)
    @GetMapping("/page")
    public R<PageResult<Material>> page(MaterialService.MaterialQuery query) {
        return R.ok(materialService.page(query));
    }

    /** 下单可选商品：门店 / 加盟商 / 总部均可读 */
    @SaCheckRole(value = {"HQ_ADMIN", "HQ_OPS", "HQ_FIN", "FRANCHISEE_ADMIN", "STORE_OP"}, mode = SaMode.OR)
    @GetMapping("/options")
    public R<List<Material>> options() {
        return R.ok(materialService.options());
    }

    /** 新增 / 编辑商品（含库存设置，库存留空表示无限） */
    @SaCheckRole(value = {"HQ_ADMIN", "HQ_OPS"}, mode = SaMode.OR)
    @PostMapping("/save")
    public R<Void> save(@RequestBody MaterialService.MaterialSaveReq req) {
        materialService.save(req);
        return R.ok(null);
    }

    /** 启用 / 停用商品 */
    @SaCheckRole(value = {"HQ_ADMIN", "HQ_OPS"}, mode = SaMode.OR)
    @PutMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @RequestBody StatusReq req) {
        materialService.changeStatus(id, req.active());
        return R.ok(null);
    }

    public record StatusReq(boolean active) {
        public boolean getActive() {
            return active;
        }
    }
}
