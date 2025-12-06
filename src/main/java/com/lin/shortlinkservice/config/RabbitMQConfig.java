package com.lin.shortlinkservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * 用于消息队列的削峰填谷,实现异步处理访问日志
 * 这是企业级高并发架构的核心!
 */
@Configuration
public class RabbitMQConfig {
    
    /**
     * 访问日志队列名称
     */
    public static final String ACCESS_LOG_QUEUE = "short_link.access_log.queue";
    
    /**
     * 访问日志交换机名称
     */
    public static final String ACCESS_LOG_EXCHANGE = "short_link.access_log.exchange";
    
    /**
     * 路由键
     */
    public static final String ACCESS_LOG_ROUTING_KEY = "short_link.access_log";
    
    /**
     * 死信队列(处理失败的消息)
     */
    public static final String ACCESS_LOG_DLQ = "short_link.access_log.dlq";
    
    /**
     * 死信交换机
     */
    public static final String ACCESS_LOG_DLX = "short_link.access_log.dlx";
    
    /**
     * 创建访问日志队列
     * 配置死信队列,确保消息不丢失
     */
    @Bean
    public Queue accessLogQueue() {
        return QueueBuilder.durable(ACCESS_LOG_QUEUE)
                .withArgument("x-dead-letter-exchange", ACCESS_LOG_DLX)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }
    
    /**
     * 创建访问日志交换机(直连模式)
     */
    @Bean
    public DirectExchange accessLogExchange() {
        return new DirectExchange(ACCESS_LOG_EXCHANGE, true, false);
    }
    
    /**
     * 绑定队列到交换机
     */
    @Bean
    public Binding accessLogBinding(Queue accessLogQueue, DirectExchange accessLogExchange) {
        return BindingBuilder.bind(accessLogQueue)
                .to(accessLogExchange)
                .with(ACCESS_LOG_ROUTING_KEY);
    }
    
    /**
     * 死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(ACCESS_LOG_DLQ).build();
    }
    
    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(ACCESS_LOG_DLX, true, false);
    }
    
    /**
     * 死信绑定
     */
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with("dlq");
    }
}
