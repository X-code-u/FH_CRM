package com.fangu.middle.org;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StoreMapper extends BaseMapper<Store> {

    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(store_code, 2) AS INTEGER)), 0) FROM store")
    int maxCodeSeq();
}
