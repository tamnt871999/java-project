package com.example.leaderboard;

import com.example.leaderboard.config.RedisConfig;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

public abstract class RedisTestBase {

    private static final String HUONG_DAN =
            "Khong ket noi duoc hai Redis o localhost:6379 va localhost:6380. "
                    + "Chay 'docker compose up -d' trong thu muc Day11-12_Redis truoc khi chay test.";

    @Autowired
    @Qualifier(RedisConfig.MASTER)
    protected StringRedisTemplate master;

    @Autowired
    @Qualifier(RedisConfig.REPLICA)
    protected StringRedisTemplate replica;

    @Value("${app.leaderboard.key}")
    protected String key;

    @BeforeEach
    void doiHaiRedisSanSangRoiXoaBangXepHang() {
        try {
            master.execute((RedisCallback<String>) connection -> connection.ping());
            replica.execute((RedisCallback<String>) connection -> connection.ping());
        } catch (RuntimeException ex) {
            throw new IllegalStateException(HUONG_DAN, ex);
        }
        master.delete(key);
        doiReplicaBatKip();
    }

    protected void doiReplicaBatKip() {
        for (int lan = 0; lan < 150; lan++) {
            long cuaMaster = truongSo(tren(master, "INFO", "replication"), "master_repl_offset:");
            long cuaReplica = truongSo(tren(replica, "INFO", "replication"), "slave_repl_offset:");
            if (cuaReplica >= cuaMaster) {
                return;
            }
            ngu(20);
        }
        throw new IllegalStateException("Replica khong bat kip master sau 3 giay");
    }

    protected String tren(StringRedisTemplate template, String... command) {
        byte[][] args = new byte[command.length - 1][];
        for (int i = 1; i < command.length; i++) {
            args[i - 1] = command[i].getBytes(StandardCharsets.UTF_8);
        }
        Object raw = template.execute(
                (RedisCallback<Object>) connection -> connection.execute(command[0], args));
        return raw instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : String.valueOf(raw);
    }

    private static long truongSo(String info, String truong) {
        for (String dong : info.split("\\R")) {
            if (dong.startsWith(truong)) {
                return Long.parseLong(dong.substring(truong.length()).trim());
            }
        }
        throw new IllegalStateException("Khong thay truong " + truong + " trong INFO replication");
    }

    private static void ngu(long mili) {
        try {
            Thread.sleep(mili);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
