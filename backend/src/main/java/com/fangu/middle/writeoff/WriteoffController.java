package com.fangu.middle.writeoff;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.fangu.middle.common.PageResult;
import com.fangu.middle.common.R;
import com.fangu.middle.datascope.DataScope;
import com.fangu.middle.writeoff.WriteoffService.WriteoffDetailVO;
import com.fangu.middle.writeoff.WriteoffService.WriteoffReq;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 套餐核销：门店选择套餐 -> 自动带出商品 -> 确认核销并扣减库存 */
@RestController
@RequestMapping("/writeoff")
@RequiredArgsConstructor
public class WriteoffController {

    private final WriteoffService writeoffService;

    @SaCheckRole(value = {"FRANCHISEE_ADMIN", "STORE_OP"}, mode = SaMode.OR)
    @DataScope
    @GetMapping("/page")
    public R<PageResult<WriteoffVO>> page(WriteoffQuery query) {
        return R.ok(writeoffService.page(query.getPageNum(), query.getPageSize(), query.getPackageName()));
    }

    @SaCheckRole(value = {"FRANCHISEE_ADMIN", "STORE_OP"}, mode = SaMode.OR)
    @DataScope
    @GetMapping("/{id}")
    public R<WriteoffDetailVO> detail(@PathVariable Long id) {
        return R.ok(writeoffService.detail(id));
    }

    @SaCheckRole(value = {"FRANCHISEE_ADMIN", "STORE_OP"}, mode = SaMode.OR)
    @DataScope
    @PostMapping
    public R<Long> writeoff(@RequestBody WriteoffReq req) {
        return R.ok(writeoffService.writeoff(req));
    }

    @Data
    public static class WriteoffQuery {
        private long pageNum = 1;
        private long pageSize = 10;
        private String packageName;
    }
}
