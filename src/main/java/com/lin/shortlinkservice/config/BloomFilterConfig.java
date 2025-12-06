package com.lin.shortlinkservice.config;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * 布隆过滤器配置
 * 用于快速判断短链接是否存在,防止缓存穿透
 * 这是面试亮点!
 */
@Configuration
public class BloomFilterConfig {
    
    /**
     * 预期插入的数据量
     */
    private static final int EXPECTED_INSERTIONS = 10_000_000;
    
    /**
     * 可接受的误判率 (0.01 = 1%)
     */
    private static final double FALSE_POSITIVE_PROBABILITY = 0.01;
    
    /**
     * 创建布隆过滤器Bean
     * 
     * 原理:
     * - 使用多个哈希函数将元素映射到位数组
     * - 判断元素存在时,检查对应位置是否都为1
     * - 如果都为1,元素可能存在(有误判率)
     * - 如果有0,元素一定不存在(零漏判率)
     * 
     * @return BloomFilter实例
     */
    @Bean
    public BloomFilter<String> shortCodeBloomFilter() {
        return BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS,
                FALSE_POSITIVE_PROBABILITY
        );
    }
}
