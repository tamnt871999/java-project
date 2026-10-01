package com.example.leaderboard.service;

import com.example.leaderboard.config.RedisConfig;
import com.example.leaderboard.service.ProductService.ProductView;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductFacade {

    private final ProductService productService;
    private final ProductLocalCache localCache;
    private final StringRedisTemplate master;
    private final String deletionChannel;

    public ProductFacade(ProductService productService,
                         ProductLocalCache localCache,
                         @Qualifier(RedisConfig.MASTER) StringRedisTemplate master,
                         @Value("${app.product.deletion-channel}") String deletionChannel) {
        this.productService = productService;
        this.localCache = localCache;
        this.master = master;
        this.deletionChannel = deletionChannel;
    }

    public ProductView findById(Long id) {
        return localCache.get(id).orElseGet(() -> {
            ProductView loaded = productService.findById(id);
            localCache.put(loaded);
            return loaded;
        });
    }

    public void delete(Long id) {
        productService.delete(id);
        localCache.evict(id);
        master.convertAndSend(deletionChannel, String.valueOf(id));
    }
}
