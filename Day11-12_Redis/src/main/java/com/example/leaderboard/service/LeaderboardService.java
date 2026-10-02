package com.example.leaderboard.service;

import com.example.leaderboard.config.RedisConfig;
import com.example.leaderboard.exception.BusinessRuleException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardService {

    private final StringRedisTemplate master;
    private final StringRedisTemplate replica;
    private final String key;

    public LeaderboardService(@Qualifier(RedisConfig.MASTER) StringRedisTemplate master,
                              @Qualifier(RedisConfig.REPLICA) StringRedisTemplate replica,
                              @Value("${app.leaderboard.key}") String key) {
        this.master = master;
        this.replica = replica;
        this.key = key;
    }

    public PlayerScore submitScore(String username, double score) {
        String player = requireUsername(username);
        master.opsForZSet().add(key, player, score);
        return new PlayerScore(player, score);
    }

    public List<PlayerScore> topN(int n) {
        if (n <= 0) {
            throw new BusinessRuleException("So luong nguoi choi can lay phai lon hon 0, dang nhan " + n);
        }
        Set<TypedTuple<String>> rows = replica.opsForZSet().reverseRangeWithScores(key, 0, n - 1);
        List<PlayerScore> top = new ArrayList<>();
        if (rows == null) {
            return top;
        }
        for (TypedTuple<String> row : rows) {
            top.add(new PlayerScore(row.getValue(), row.getScore()));
        }
        return top;
    }

    private String requireUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessRuleException("Ten nguoi choi khong duoc de trong");
        }
        return username.trim();
    }

    public record PlayerScore(String username, Double score) {
    }
}
