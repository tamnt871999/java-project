package com.example.leaderboard.service;

import java.nio.charset.StandardCharsets;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class ProductDeletionListener implements MessageListener {

    private final ProductLocalCache localCache;

    public ProductDeletionListener(ProductLocalCache localCache) {
        this.localCache = localCache;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        Long id = Long.valueOf(new String(message.getBody(), StandardCharsets.UTF_8));
        localCache.evict(id);
    }
}
