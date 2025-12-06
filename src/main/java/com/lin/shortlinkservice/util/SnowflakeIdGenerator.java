package com.lin.shortlinkservice.util;

import org.springframework.stereotype.Component;

/**
 * 雪花算法ID生成器 (简化版)
 * 用于分布式环境下生成唯一ID
 * 这是面试高频考点!
 * 
 * 雪花算法ID结构(64位):
 * 1位符号位(0) + 41位时间戳 + 10位工作机器ID + 12位序列号
 */
@Component
public class SnowflakeIdGenerator {
    
    // 起始时间戳 (2024-01-01 00:00:00)
    private static final long START_TIMESTAMP = 1704067200000L;
    
    // 各部分位数
    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    
    // 最大值
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    
    // 位移量
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS;
    
    // 工作机器ID (0-1023)
    private final long workerId;
    
    // 序列号
    private long sequence = 0L;
    
    // 上次生成ID的时间戳
    private long lastTimestamp = -1L;
    
    /**
     * 构造函数
     * @param workerId 工作机器ID (0-1023)
     */
    public SnowflakeIdGenerator() {
        // 简化版:使用进程ID作为workerId
        this.workerId = getWorkerId();
    }
    
    /**
     * 生成唯一ID (线程安全)
     */
    public synchronized long nextId() {
        long timestamp = currentTimestamp();
        
        // 时钟回拨检测
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id");
        }
        
        // 同一毫秒内,序列号递增
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 序列号溢出,等待下一毫秒
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒,序列号重置
            sequence = 0L;
        }
        
        lastTimestamp = timestamp;
        
        // 组装64位ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }
    
    /**
     * 等待下一毫秒
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimestamp();
        }
        return timestamp;
    }
    
    /**
     * 获取当前时间戳
     */
    private long currentTimestamp() {
        return System.currentTimeMillis();
    }
    
    /**
     * 获取工作机器ID (简化实现)
     */
    private long getWorkerId() {
        try {
            // 使用进程ID的后10位作为workerId
            String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            long pid = Long.parseLong(processName.split("@")[0]);
            return pid & MAX_WORKER_ID;
        } catch (Exception e) {
            // 失败则使用随机数
            return (long) (Math.random() * MAX_WORKER_ID);
        }
    }
}
