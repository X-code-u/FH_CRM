package com.fangu.middle.sale;

import com.fangu.middle.common.BizException;
import com.fangu.middle.material.Material;
import com.fangu.middle.material.MaterialLevelPrice;
import com.fangu.middle.material.MaterialLevelPriceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 定价解析（规则 R1，纯解析不落库）：
 * 直营门店 -> direct_price (DIRECT)
 * 加盟门店 -> 等级价 (LEVEL) -> 缺失回退基准价 (BASE)
 * 解析结果 <= 0 由调用方拦截建单。
 */
@Component
@RequiredArgsConstructor
public class PricingResolver {

    private final MaterialLevelPriceMapper levelPriceMapper;

    public PriceResult resolve(Material material, String storeType, String levelCode) {
        if ("DIRECT".equals(storeType)) {
            return new PriceResult(material.getDirectPrice(), "DIRECT");
        }
        if (levelCode != null && !levelCode.isBlank()) {
            MaterialLevelPrice levelPrice = levelPriceMapper.selectOne(
                    com.baomidou.mybatisplus.core.toolkit.Wrappers.<MaterialLevelPrice>lambdaQuery()
                            .eq(MaterialLevelPrice::getMaterialId, material.getId())
                            .eq(MaterialLevelPrice::getLevelCode, levelCode));
            if (levelPrice != null && levelPrice.getSalePrice() != null) {
                return new PriceResult(levelPrice.getSalePrice(), "LEVEL");
            }
        }
        return new PriceResult(material.getBaseSalePrice(), "BASE");
    }

    public void ensurePositive(BigDecimal price, String materialName) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(40003, "商品「" + materialName + "」价格未维护，无法下单");
        }
    }

    public record PriceResult(BigDecimal price, String source) {
    }
}
