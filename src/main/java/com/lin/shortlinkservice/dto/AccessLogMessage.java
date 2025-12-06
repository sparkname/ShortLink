package com.lin.shortlinkservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 访问日志消息DTO
 * 用于 RabbitMQ 消息传递
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessLogMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
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
     * 访问时间戳
     */
    private Long accessTimestamp;
}
