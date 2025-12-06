package com.lin.shortlinkservice.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.config.algorithm.AlgorithmConfiguration;
import org.apache.shardingsphere.readwritesplitting.api.ReadwriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.api.rule.ReadwriteSplittingDataSourceRuleConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

/**
 * ShardingSphere 读写分离配置
 * 实现主从数据库的自动路由
 * 这是企业级架构的标准配置!
 * 
 * 核心原理:
 * - 写操作(INSERT/UPDATE/DELETE) -> 主库
 * - 读操作(SELECT) -> 从库(负载均衡)
 * 
 * 注意: 仅在 sharding profile 下启用
 */
@Configuration
@Profile("sharding")  // 只在 sharding 环境下启用
public class ShardingSphereConfig {
    
    @Value("${spring.datasource.master.url}")
    private String masterUrl;
    
    @Value("${spring.datasource.master.username}")
    private String masterUsername;
    
    @Value("${spring.datasource.master.password}")
    private String masterPassword;
    
    @Value("${spring.datasource.slave.url}")
    private String slaveUrl;
    
    @Value("${spring.datasource.slave.username}")
    private String slaveUsername;
    
    @Value("${spring.datasource.slave.password}")
    private String slavePassword;
    
    /**
     * 创建主库数据源
     */
    private DataSource createMasterDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setJdbcUrl(masterUrl);
        dataSource.setUsername(masterUsername);
        dataSource.setPassword(masterPassword);
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(5);
        return dataSource;
    }
    
    /**
     * 创建从库数据源
     */
    private DataSource createSlaveDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setJdbcUrl(slaveUrl);
        dataSource.setUsername(slaveUsername);
        dataSource.setPassword(slavePassword);
        dataSource.setMaximumPoolSize(50);
        dataSource.setMinimumIdle(10);
        return dataSource;
    }
    
    /**
     * 配置 ShardingSphere 数据源
     * 
     * 面试重点:
     * 1. 主库负责写,从库负责读
     * 2. 从库可以配置多个,自动负载均衡
     * 3. 基于 Binlog 主从复制同步数据
     */
    @Bean
    @Primary
    public DataSource dataSource() throws SQLException {
        // 1. 创建真实数据源 Map
        Map<String, DataSource> dataSourceMap = new HashMap<>();
        dataSourceMap.put("master", createMasterDataSource());
        dataSourceMap.put("slave", createSlaveDataSource());
        
        // 2. 配置读写分离规则
        ReadwriteSplittingDataSourceRuleConfiguration dataSourceConfig = 
            new ReadwriteSplittingDataSourceRuleConfiguration(
                "shortLinkDataSource",  // 逻辑数据源名称
                "master",               // 写数据源(主库)
                Arrays.asList("slave"), // 读数据源列表(从库)
                "round_robin"           // 负载均衡算法
            );
        
        // 3. 配置负载均衡算法
        Map<String, AlgorithmConfiguration> loadBalancers = new HashMap<>();
        loadBalancers.put("round_robin", 
            new AlgorithmConfiguration("ROUND_ROBIN", new Properties()));
        
        // 4. 创建读写分离规则配置
        ReadwriteSplittingRuleConfiguration ruleConfig = 
            new ReadwriteSplittingRuleConfiguration(
                Collections.singleton(dataSourceConfig),
                loadBalancers
            );
        
        // 5. 创建 ShardingSphere 数据源
        return ShardingSphereDataSourceFactory.createDataSource(
            dataSourceMap,
            Collections.singleton(ruleConfig),
            new Properties()
        );
    }
}
