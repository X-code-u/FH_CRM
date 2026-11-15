package com.fangu.middle.stock;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangu.middle.common.PageResult;
import com.fangu.middle.common.R;
import com.fangu.middle.datascope.DataScope;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockQueryMapper stockQueryMapper;

    /** 门店库存台账：门店账号仅本门店，总部可看全部并按门店筛选 */
    @SaCheckRole(value = {"HQ_ADMIN", "HQ_OPS", "HQ_FIN", "STORE_OP"}, mode = SaMode.OR)
    @DataScope
    @GetMapping("/page")
    public R<PageResult<StockVO>> page(StockQuery query) {
        IPage<StockVO> page = stockQueryMapper.selectStockPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query);
        return R.ok(PageResult.of(page));
    }

    @Data
    public static class StockQuery {
        private long pageNum = 1;
        private long pageSize = 10;
        private Long storeId;
        private String storeName;
        private String materialName;
    }
}
