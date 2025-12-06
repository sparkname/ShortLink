package com.lin.shortlinkservice.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * 简化的数据源配置(用于快速启动和测试)
 * 如需读写分离,请使用 ShardingSphereConfig
 */
@Configuration
@Profile("!sharding")  // 只在非 sharding 环境下启用
public class SimpleDataSourceConfig {
    
    @Value("${spring.datasource.master.url}")
    private String url;
    
    @Value("${spring.datasource.master.username}")
    private String username;
    
    @Value("${spring.datasource.master.password}")
    private String password;
    
    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(20);
        dataSource.setMinimumIdle(5);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);
        return dataSource;
    }
}
