package com.lin.shortlinkservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * 应用测试类
 * 
 * 注意: 企业级版本需要 RabbitMQ、Redis、MySQL 才能完整启动
 * 这里使用测试配置跳过外部依赖
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
    "spring.datasource.master.url=jdbc:h2:mem:testdb",
    "spring.datasource.slave.url=jdbc:h2:mem:testdb"
})
class ShortLinkServiceApplicationTests {

    @Test
    void contextLoads() {
        // 测试 Spring 上下文是否能正常加载
    }

}
