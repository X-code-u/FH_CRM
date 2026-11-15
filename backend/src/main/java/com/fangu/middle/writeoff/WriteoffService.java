package com.fangu.middle.writeoff;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fangu.middle.auth.AuthService;
import com.fangu.middle.common.BizException;
import com.fangu.middle.common.PageResult;
import com.fangu.middle.pkg.PackageItemVO;
import com.fangu.middle.pkg.PackageService;
import com.fangu.middle.pkg.StorePackage;
import com.fangu.middle.stock.StockService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 套餐核销：固定消耗商品必扣，可选商品由门店勾选数量，核销后自动扣减库存 */
@Service
@RequiredArgsConstructor
public class WriteoffService {

    private static final DateTimeFormatter DATE_PART = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StoreWriteoffMapper writeoffMapper;
    private final StoreWriteoffItemMapper itemMapper;
    private final WriteoffQueryMapper queryMapper;
    private final PackageService packageService;
    private final StockService stockService;
    private final AuthService authService;

    public PageResult<WriteoffVO> page(long pageNum, long pageSize, String packageName) {
        return PageResult.of(queryMapper.selectPage(
                new Page<>(pageNum, pageSize),
                packageName == null || packageName.isBlank() ? null : packageName));
    }

    public WriteoffDetailVO detail(Long id) {
        StoreWriteoff writeoff = writeoffMapper.selectById(id);
        if (writeoff == null) {
            throw new BizException(40001, "核销记录不存在或无权访问");
        }
        WriteoffDetailVO vo = new WriteoffDetailVO();
        vo.setWriteoff(writeoff);
        vo.setItems(queryMapper.selectItems(id));
        return vo;
    }

    @Transactional
    public Long writeoff(WriteoffReq req) {
        if (req.getPackageId() == null) {
            throw new BizException(40001, "请选择套餐");
        }
        if (req.getRequestNo() == null || req.getRequestNo().isBlank()) {
            throw new BizException(40001, "缺少幂等号 requestNo");
        }
        StoreWriteoff exists = writeoffMapper.selectOne(Wrappers.<StoreWriteoff>lambdaQuery()
                .eq(StoreWriteoff::getRequestNo, req.getRequestNo()));
        if (exists != null) {
            return exists.getId();
        }

        PackageService.PackageDetailVO detail = packageService.requireEnabled(req.getPackageId());
        StorePackage pkg = detail.getPackageInfo();

        Map<Long, BigDecimal> optionalQty = new LinkedHashMap<>();
        if (req.getItems() != null) {
            for (ItemReq item : req.getItems()) {
                if (item.getMaterialId() == null || item.getQty() == null
                        || item.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                optionalQty.put(item.getMaterialId(), item.getQty());
            }
        }

        List<StockService.OutboundItem> outbound = new ArrayList<>();
        List<StoreWriteoffItem> records = new ArrayList<>();
        for (PackageItemVO pkgItem : detail.getItems()) {
            boolean fixed = !"OPTIONAL".equals(pkgItem.getItemType());
            BigDecimal qty = fixed ? pkgItem.getQty() : optionalQty.get(pkgItem.getMaterialId());
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            outbound.add(new StockService.OutboundItem(
                    pkgItem.getMaterialId(), qty, pkgItem.getMaterialName()));
            StoreWriteoffItem record = new StoreWriteoffItem();
            record.setStoreId(pkg.getStoreId());
            record.setFranchiseeId(pkg.getFranchiseeId());
            record.setMaterialId(pkgItem.getMaterialId());
            record.setQty(qty);
            record.setItemType(fixed ? "FIXED" : "OPTIONAL");
            records.add(record);
        }

        StoreWriteoff writeoff = new StoreWriteoff();
        writeoff.setStoreId(pkg.getStoreId());
        writeoff.setFranchiseeId(pkg.getFranchiseeId());
        writeoff.setPackageId(pkg.getId());
        writeoff.setPackageName(pkg.getPackageName());
        writeoff.setPackagePrice(pkg.getPrice() == null ? BigDecimal.ZERO : pkg.getPrice());
        writeoff.setBeautician(req.getBeautician() == null || req.getBeautician().isBlank()
                ? null : req.getBeautician().trim());
        writeoff.setRequestNo(req.getRequestNo());
        writeoff.setRemark(req.getRemark());
        writeoff.setCreatedBy(authService.current().getUserId());
        insertWithNo(writeoff);

        // 扣减库存并写流水（结存不足会抛异常，整个核销回滚）
        stockService.outbound(pkg.getStoreId(), writeoff.getId(), outbound, req.getRequestNo());

        for (StoreWriteoffItem record : records) {
            record.setWriteoffId(writeoff.getId());
            itemMapper.insert(record);
        }
        return writeoff.getId();
    }

    private void insertWithNo(StoreWriteoff writeoff) {
        String datePart = LocalDate.now().format(DATE_PART);
        for (int attempt = 0; attempt < 2; attempt++) {
            writeoff.setWriteoffNo("HX" + datePart
                    + String.format("%04d", writeoffMapper.maxSeq(datePart) + 1));
            try {
                writeoffMapper.insert(writeoff);
                return;
            } catch (DuplicateKeyException e) {
                if (attempt == 1) {
                    throw new BizException(40005, "核销单号生成并发冲突，请重试");
                }
            }
        }
    }

    @Data
    public static class WriteoffReq {
        private Long packageId;
        private String requestNo;
        private String beautician;
        private String remark;
        private List<ItemReq> items;
    }

    @Data
    public static class ItemReq {
        private Long materialId;
        private BigDecimal qty;
    }

    @Data
    public static class WriteoffDetailVO {
        private StoreWriteoff writeoff;
        private List<WriteoffItemVO> items;
    }
}
