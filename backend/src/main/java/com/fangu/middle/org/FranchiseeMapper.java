package com.fangu.middle.org;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FranchiseeMapper extends BaseMapper<Franchisee> {

    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(franchisee_code, 2) AS INTEGER)), 0) FROM franchisee")
    int maxCodeSeq();
}
