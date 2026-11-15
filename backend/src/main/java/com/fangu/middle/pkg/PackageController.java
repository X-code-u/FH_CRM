package com.fangu.middle.pkg;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.fangu.middle.common.PageResult;
import com.fangu.middle.common.R;
import com.fangu.middle.datascope.DataScope;
import com.fangu.middle.pkg.PackageService.PackageDetailVO;
import com.fangu.middle.pkg.PackageService.PackageReq;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 门店套餐管理：门店/加盟商账号维护本店套餐，可绑定已入库商品 */
@RestController
@RequestMapping("/package")
@RequiredArgsConstructor
public class PackageController {

    private static final String[] STORE_SIDE = {"FRANCHISEE_ADMIN", "STORE_OP"};

    private final PackageService packageService;

    @SaCheckRole(value = {"FRANCHISEE_ADMIN", "STORE_OP"}, mode = SaMode.OR)
    @DataScope
    @GetMapping("/page")
    public R<PageResult<PackageVO>> page(PackageQuery query) {
        return R.ok(packageService.page(query.getPageNum(), query.getPageSize(),
                query.getName(), query.getStatus()));
    }

    @SaCheckRole(value = {"FRANCHISEE_ADMIN", "STORE_OP"}, mode = SaMode.OR)
    @DataScope
    @GetMapping("/{id}")
    public R<PackageDetailVO> detail(@PathVariable Long id) {
        return R.ok(packageService.detail(id));
    }

    @SaCheckRole(value = {"FRANCHISEE_ADMIN", "STORE_OP"}, mode = SaMode.OR)
    @GetMapping("/material-options")
    public R<List<PackageItemVO>> materialOptions(@RequestParam(required = false) Long storeId) {
        return R.ok(packageService.materialOptions(storeId));
    }

    @SaCheckRole(value = {"FRANCHISEE_ADMIN", "STORE_OP"}, mode = SaMode.OR)
    @PostMapping
    public R<Long> create(@RequestBody PackageReq req) {
        return R.ok(packageService.create(req));
    }

    @SaCheckRole(value = {"FRANCHISEE_ADMIN", "STORE_OP"}, mode = SaMode.OR)
    @DataScope
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody PackageReq req) {
        packageService.update(id, req);
        return R.ok(null);
    }

    @SaCheckRole(value = {"FRANCHISEE_ADMIN", "STORE_OP"}, mode = SaMode.OR)
    @DataScope
    @PutMapping("/{id}/status")
    public R<Void> changeStatus(@PathVariable Long id, @RequestBody StatusReq req) {
        packageService.changeStatus(id, Boolean.TRUE.equals(req.getEnabled()));
        return R.ok(null);
    }

    @Data
    public static class PackageQuery {
        private long pageNum = 1;
        private long pageSize = 10;
        private String name;
        private String status;
    }

    @Data
    public static class StatusReq {
        private Boolean enabled;
    }
}
