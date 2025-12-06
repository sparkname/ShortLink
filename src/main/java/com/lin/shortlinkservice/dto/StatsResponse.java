package com.lin.shortlinkservice.dto;

import lombok.Data;

/**
 * 访问统计响应DTO
 */
@Data
public class StatsResponse {
    
    /**
     * 短链接码
     */
    private String shortCode;
    
    /**
     * 原始长链接
     */
    private String originalUrl;
    
    /**
     * 页面浏览量
     */
    private Long pv;
    
    /**
     * 独立访客数
     */
    private Long uv;
}
