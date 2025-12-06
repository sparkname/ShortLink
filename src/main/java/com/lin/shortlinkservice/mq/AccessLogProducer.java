package com.lin.shortlinkservice.mq;

import com.lin.shortlinkservice.config.RabbitMQConfig;
import com.lin.shortlinkservice.dto.AccessLogMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 访问日志消息生产者
 * 负责将访问日志发送到 RabbitMQ
 * 
 * 面试话术:
 * "用户访问短链接时,我立即返回跳转,同时将访问信息以消息的形式发送到 MQ。
 *  即使瞬间涌入百万级点击,MQ 也能削峰填谷,让后台消费者慢慢处理,
 *  绝不会阻塞用户的跳转请求。"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogProducer {
    
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 发送访问日志消息到 MQ
     * 
     * @param message 访问日志消息
     */
    public void sendAccessLog(AccessLogMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ACCESS_LOG_EXCHANGE,
                    RabbitMQConfig.ACCESS_LOG_ROUTING_KEY,
                    message
            );
            log.debug("发送访问日志到MQ成功: {}", message.getShortCode());
        } catch (Exception e) {
            log.error("发送访问日志到MQ失败: {}", message.getShortCode(), e);
            // 可以在这里添加降级策略,比如写入本地文件
        }
    }
}
