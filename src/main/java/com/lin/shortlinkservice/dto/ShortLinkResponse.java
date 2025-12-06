package com.lin.shortlinkservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短链接响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortLinkResponse {
    
    /**
     * 短链接完整URL
     */
    private String shortUrl;
    
    /**
     * 短链接码
     */
    private String shortCode;
    
    /**
     * 原始长链接
     */
    private String originalUrl;
}
