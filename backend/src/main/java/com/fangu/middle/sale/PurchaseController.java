package com.fangu.middle.sale;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.fangu.middle.common.PageResult;
import com.fangu.middle.common.R;
import com.fangu.middle.datascope.DataScope;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    /** 门店下单：门店操作员 / 加盟商管理员 */
    @SaCheckRole(value = {"FRANCHISEE_ADMIN", "STORE_OP"}, mode = SaMode.OR)
    @PostMapping("/orders")
    public R<String> create(@RequestBody PurchaseService.CreateReq req) {
        return R.ok(purchaseService.create(req));
    }

    /** 采购单分页：门店/加盟商只看自己的，总部看全部（SQL 层追加归属条件） */
    @SaCheckRole(value = {"HQ_ADMIN", "HQ_OPS", "HQ_FIN", "FRANCHISEE_ADMIN", "STORE_OP"}, mode = SaMode.OR)
    @DataScope
    @GetMapping("/orders/page")
    public R<PageResult<PurchaseService.OrderVO>> page(PurchaseService.OrderQuery query) {
        return R.ok(purchaseService.page(query));
    }

    /** 采购单详情：越权单据查不到 */
    @SaCheckRole(value = {"HQ_ADMIN", "HQ_OPS", "HQ_FIN", "FRANCHISEE_ADMIN", "STORE_OP"}, mode = SaMode.OR)
    @DataScope
    @GetMapping("/orders/{id}")
    public R<PurchaseService.OrderDetailVO> detail(@PathVariable Long id) {
        return R.ok(purchaseService.detail(id));
    }

    /** 审批通过：总部可修改明细数量 */
    @SaCheckRole(value = {"HQ_ADMIN", "HQ_OPS"}, mode = SaMode.OR)
    @PutMapping("/orders/{id}/approve")
    public R<Void> approve(@PathVariable Long id, @RequestBody PurchaseService.ApproveReq req) {
        purchaseService.approve(id, req);
        return R.ok(null);
    }

    /** 审批驳回：总部填写驳回原因 */
    @SaCheckRole(value = {"HQ_ADMIN", "HQ_OPS"}, mode = SaMode.OR)
    @PutMapping("/orders/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @RequestBody RejectReq req) {
        purchaseService.reject(id, req.getReason());
        return R.ok(null);
    }

    @Data
    public static class RejectReq {
        private String reason;
    }
}
