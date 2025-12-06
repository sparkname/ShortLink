package com.lin.shortlinkservice.dto;

import lombok.Data;

/**
 * 生成短链接请求DTO
 */
@Data
public class CreateShortLinkRequest {
    
    /**
     * 原始长链接
     */
    private String originalUrl;
    
    /**
     * 过期天数(可选)
     */
    private Integer expireDays;
}
