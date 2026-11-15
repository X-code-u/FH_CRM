package com.fangu.middle.writeoff;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StoreWriteoffMapper extends BaseMapper<StoreWriteoff> {

    /** 当日核销单号流水最大值（单号格式 HX + yyyyMMdd + 4 位流水） */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(writeoff_no, 11) AS SIGNED)), 0) FROM store_writeoff "
            + "WHERE writeoff_no LIKE CONCAT('HX', #{datePart}, '%')")
    int maxSeq(@Param("datePart") String datePart);
}
