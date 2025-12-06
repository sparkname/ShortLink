package com.lin.shortlinkservice.mq;

import com.lin.shortlinkservice.config.RabbitMQConfig;
import com.lin.shortlinkservice.dto.AccessLogMessage;
import com.lin.shortlinkservice.entity.AccessLog;
import com.lin.shortlinkservice.mapper.AccessLogMapper;
import com.lin.shortlinkservice.mapper.AccessStatsMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * 访问日志消息消费者
 * 负责从 RabbitMQ 消费访问日志并处理
 * 
 * 这是独立的数据分析系统,专门处理访问统计
 * 与核心跳转业务完全解耦!
 * 
 * 面试话术:
 * "我设计了一个独立的消费者服务,从 MQ 中取出访问消息慢慢处理。
 *  这个消费者可以独立部署,独立扩容,完全不影响跳转服务的性能。
 *  即使消费者暂时挂了,消息也会在 MQ 中保存,不会丢失。"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogConsumer {
    
    private final AccessLogMapper accessLogMapper;
    private final AccessStatsMapper accessStatsMapper;
    private final StringRedisTemplate redisTemplate;
    
    private static final String UV_SET_PREFIX = "uv:";
    private static final int CACHE_EXPIRE_DAYS = 30;
    
    /**
     * 消费访问日志消息
     * 
     * @param message 访问日志消息
     * @param channel RabbitMQ 通道
     * @param deliveryTag 消息标识
     */
    @RabbitListener(queues = RabbitMQConfig.ACCESS_LOG_QUEUE)
    public void consumeAccessLog(
            @Payload AccessLogMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        
        try {
            log.info("开始处理访问日志: {}", message.getShortCode());
            
            // 1. 记录访问日志到数据库(写主库)
            AccessLog accessLog = new AccessLog();
            accessLog.setShortCode(message.getShortCode());
            accessLog.setIp(message.getIp());
            accessLog.setUserAgent(message.getUserAgent());
            accessLog.setReferer(message.getReferer());
            accessLog.setAccessTime(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(message.getAccessTimestamp()),
                    ZoneId.systemDefault()
            ));
            accessLogMapper.insert(accessLog);
            
            // 2. 更新 PV(页面浏览量) - 每次访问 +1
            accessStatsMapper.incrementPv(message.getShortCode());
            
            // 3. 更新 UV(独立访客数) - 使用 Redis Set 去重
            String uvKey = UV_SET_PREFIX + message.getShortCode();
            Boolean isNewVisitor = redisTemplate.opsForSet().add(uvKey, message.getIp()) != null;
            
            if (Boolean.TRUE.equals(isNewVisitor)) {
                // 新访客,UV +1
                accessStatsMapper.incrementUv(message.getShortCode());
                redisTemplate.expire(uvKey, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            }
            
            // 4. 手动确认消息(ACK)
            channel.basicAck(deliveryTag, false);
            log.info("访问日志处理成功: {}", message.getShortCode());
            
        } catch (Exception e) {
            log.error("处理访问日志失败: {}", message.getShortCode(), e);
            try {
                // 5. 处理失败,拒绝消息并重新入队(最多重试3次)
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("拒绝消息失败", ex);
            }
        }
    }
    
    /**
     * 消费死信队列中的消息
     * 这些是处理失败超过重试次数的消息
     */
    @RabbitListener(queues = RabbitMQConfig.ACCESS_LOG_DLQ)
    public void consumeDeadLetter(
            @Payload AccessLogMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        
        try {
            log.warn("处理死信队列消息: {}", message.getShortCode());
            
            // 这里可以:
            // 1. 记录到特殊的错误表
            // 2. 发送告警通知
            // 3. 写入日志文件供后续分析
            
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理死信消息失败", e);
        }
    }
}
