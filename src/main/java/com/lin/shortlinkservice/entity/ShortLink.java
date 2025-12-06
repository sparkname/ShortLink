package com.lin.shortlinkservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 短链接实体类
 */
@Data
@TableName("short_link")
public class ShortLink {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 短链接码(Base62编码)
     */
    private String shortCode;
    
    /**
     * 原始长链接
     */
    private String originalUrl;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    
    /**
     * 状态: 1-正常, 0-失效
     */
    private Integer status;
}
