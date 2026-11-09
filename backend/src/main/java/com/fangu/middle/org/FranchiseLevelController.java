package com.fangu.middle.org;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.fangu.middle.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/franchise-level")
@RequiredArgsConstructor
public class FranchiseLevelController {

    private final LevelService levelService;

    /** 等级列表：总部角色可见 */
    @SaCheckRole(value = {"HQ_ADMIN", "HQ_OPS", "HQ_FIN"}, mode = SaMode.OR)
    @GetMapping("/list")
    public R<List<FranchiseLevel>> list() {
        return R.ok(levelService.list());
    }

    /** 新增或修改等级：仅总部超管 */
    @SaCheckRole("HQ_ADMIN")
    @PostMapping("/save")
    public R<Void> save(@RequestBody LevelService.LevelSaveReq req) {
        levelService.save(req);
        return R.ok(null);
    }
}
