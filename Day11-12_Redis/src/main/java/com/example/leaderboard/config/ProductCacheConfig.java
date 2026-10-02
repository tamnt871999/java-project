package com.example.leaderboard.config;

import com.example.leaderboard.service.ProductDeletionListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@EnableCaching
public class ProductCacheConfig {

    @Bean
    public RedisMessageListenerContainer productDeletionListenerContainer(
            @Qualifier("masterConnectionFactory") RedisConnectionFactory factory,
            ProductDeletionListener listener,
            @Value("${app.product.deletion-channel}") String channel) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(listener, new ChannelTopic(channel));
        return container;
    }
}
