package com.fangu.middle.report;

import com.fangu.middle.common.R;
import com.fangu.middle.datascope.DataScope;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 数据大盘：套餐核销数量、产品核销排行、美容师业绩排行 */
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportQueryMapper reportQueryMapper;

    @DataScope
    @GetMapping("/writeoff-dashboard")
    public R<DashboardVO> writeoffDashboard(@RequestParam(defaultValue = "30") int days) {
        int range = days <= 0 ? 30 : Math.min(days, 365);
        DashboardVO vo = new DashboardVO();
        vo.setTotalWriteoffs(reportQueryMapper.countWriteoffs(range));
        vo.setTotalRevenue(reportQueryMapper.sumRevenue(range));
        vo.setTotalQty(reportQueryMapper.sumQty(range));
        vo.setPackages(reportQueryMapper.packageRank(range));
        vo.setProducts(reportQueryMapper.productRank(range));
        vo.setBeauticians(reportQueryMapper.beauticianRank(range));
        return R.ok(vo);
    }
}
