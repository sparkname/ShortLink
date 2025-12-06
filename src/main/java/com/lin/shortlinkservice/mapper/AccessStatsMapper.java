package com.lin.shortlinkservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lin.shortlinkservice.entity.AccessStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 访问统计数据访问层
 */
@Mapper
public interface AccessStatsMapper extends BaseMapper<AccessStats> {
    
    /**
     * 增加PV计数
     */
    @Update("UPDATE access_stats SET pv = pv + 1 WHERE short_code = #{shortCode}")
    int incrementPv(@Param("shortCode") String shortCode);
    
    /**
     * 增加UV计数
     */
    @Update("UPDATE access_stats SET uv = uv + 1 WHERE short_code = #{shortCode}")
    int incrementUv(@Param("shortCode") String shortCode);
}
