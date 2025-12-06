package com.lin.shortlinkservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访问日志实体类
 */
@Data
@TableName("access_log")
public class AccessLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 短链接码
     */
    private String shortCode;
    
    /**
     * 访问IP
     */
    private String ip;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 来源页面
     */
    private String referer;
    
    /**
     * 访问时间
     */
    private LocalDateTime accessTime;
    
    /**
     * 省份
     */
    private String province;
    
    /**
     * 城市
     */
    private String city;
}
