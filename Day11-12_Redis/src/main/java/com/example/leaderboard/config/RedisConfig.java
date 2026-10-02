package com.example.leaderboard.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    public static final String MASTER = "masterRedisTemplate";
    public static final String REPLICA = "replicaRedisTemplate";

    @Bean
    @Primary
    public RedisConnectionFactory masterConnectionFactory(
            @Value("${app.redis.master.host}") String host,
            @Value("${app.redis.master.port}") int port) {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
    }

    @Bean
    public RedisConnectionFactory replicaConnectionFactory(
            @Value("${app.redis.replica.host}") String host,
            @Value("${app.redis.replica.port}") int port) {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
    }

    @Bean(MASTER)
    @Primary
    public StringRedisTemplate masterRedisTemplate(
            @Qualifier("masterConnectionFactory") RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean(REPLICA)
    public StringRedisTemplate replicaRedisTemplate(
            @Qualifier("replicaConnectionFactory") RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
