package com.fangu.middle.stock;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StockFlowMapper extends BaseMapper<StockFlow> {

    /** 幂等号是否已处理 */
    @Select("SELECT COUNT(1) FROM stock_flow WHERE request_no = #{requestNo}")
    int countByRequestNo(@Param("requestNo") String requestNo);
}
