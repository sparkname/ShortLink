package com.lin.shortlinkservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访问统计实体类
 */
@Data
@TableName("access_stats")
public class AccessStats {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 短链接码
     */
    private String shortCode;
    
    /**
     * 页面浏览量
     */
    private Long pv;
    
    /**
     * 独立访客数
     */
    private Long uv;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
