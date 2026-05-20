package org.ninng.businesssvc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ninng.businesssvc.constant.CacheConstant;
import org.ninng.businesssvc.event.CacheInvalidateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Map;
import java.util.Set;

/**
 * Redis Pub/Sub 缓存失效 — 跨实例通知。
 * <p>
 * 启用方式：配置 {@code onion.cache.invalidation.redis.enabled=true}。
 * 订阅频道 {@link CacheConstant#INVALIDATION_CHANNEL}，收到失效指令后发布
 * {@link CacheInvalidateEvent.Redis} 事件，由 {@code CacheEventListener} 统一处理。
 */
@Configuration
@ConditionalOnProperty(name = "onion.cache.invalidation.redis.enabled", havingValue = "true")
public class RedisCacheInvalidationConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheInvalidationConfig.class);

    @Bean("invalidationChannel")
    ChannelTopic cacheInvalidationTopic() {
        return new ChannelTopic(CacheConstant.INVALIDATION_CHANNEL);
    }

    @Bean
    RedisMessageListenerContainer cacheInvalidationContainer(
            LettuceConnectionFactory factory,
            @Qualifier("invalidationChannel") ChannelTopic cacheInvalidationTopic,
            ApplicationEventPublisher publisher,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener((message, pattern) -> {
            var channel = new String(message.getChannel());
            log.debug("Received Redis message on channel '{}'", channel);
            try {
                var msg = objectMapper.readValue(message.getBody(), CacheInvalidateMessage.class);
                publisher.publishEvent(new CacheInvalidateEvent.Redis(msg, msg.evictions(), msg.sourceInstanceId()));
            } catch (Exception e) {
                log.error("Failed to deserialize cache invalidation message", e);
            }
        }, cacheInvalidationTopic);
        return container;
    }

    record CacheInvalidateMessage(String sourceInstanceId, Map<String, Set<Object>> evictions) {
    }
}
