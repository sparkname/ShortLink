-- 创建数据库
CREATE DATABASE IF NOT EXISTS short_link_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE short_link_db;

-- 短链接表
CREATE TABLE IF NOT EXISTS short_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    short_code VARCHAR(10) NOT NULL UNIQUE COMMENT '短链接码(Base62)',
    original_url VARCHAR(2048) NOT NULL COMMENT '原始长链接',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expire_time DATETIME COMMENT '过期时间',
    status TINYINT DEFAULT 1 COMMENT '状态:1-正常,0-失效',
    INDEX idx_short_code(short_code),
    INDEX idx_create_time(create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链接主表';

-- 访问日志表
CREATE TABLE IF NOT EXISTS access_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    short_code VARCHAR(10) NOT NULL COMMENT '短链接码',
    ip VARCHAR(64) COMMENT '访问IP',
    user_agent VARCHAR(512) COMMENT '用户代理',
    referer VARCHAR(512) COMMENT '来源页面',
    access_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    province VARCHAR(50) COMMENT '省份',
    city VARCHAR(50) COMMENT '城市',
    INDEX idx_short_code(short_code),
    INDEX idx_access_time(access_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访问日志表';

-- 访问统计表(预聚合)
CREATE TABLE IF NOT EXISTS access_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    short_code VARCHAR(10) NOT NULL COMMENT '短链接码',
    pv BIGINT DEFAULT 0 COMMENT '页面浏览量',
    uv BIGINT DEFAULT 0 COMMENT '独立访客数',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_short_code(short_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访问统计表';
