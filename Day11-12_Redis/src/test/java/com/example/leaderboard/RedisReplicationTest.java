package com.example.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.leaderboard.key=leaderboard:test")
@AutoConfigureMockMvc
class RedisReplicationTest extends RedisTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Mot container la master, container con lai la replica va dang noi duoc")
    void vaiTroHaiContainerDungNhuCauHinh() {
        String vaiTroMaster = tren(master, "INFO", "replication");
        String vaiTroReplica = tren(replica, "INFO", "replication");

        assertThat(vaiTroMaster).contains("role:master");
        assertThat(vaiTroMaster).contains("connected_slaves:1");
        assertThat(vaiTroReplica).contains("role:slave");
        assertThat(vaiTroReplica).contains("master_link_status:up");
    }

    @Test
    @DisplayName("Diem ghi vao master thi chay sang duoc replica")
    void duLieuChayTuMasterSangReplica() {
        master.opsForZSet().add(key, "an", 1200);
        doiReplicaBatKip();

        assertThat(replica.opsForZSet().score(key, "an")).isEqualTo(1200.0);
    }

    @Test
    @DisplayName("Replica tu choi moi lenh ghi vi mac dinh la read-only")
    void replicaTuChoiGhi() {
        assertThatThrownBy(() -> replica.opsForZSet().add(key, "an", 10))
                .rootCause()
                .hasMessageContaining("READONLY You can't write against a read only replica");
    }

    @Test
    @DisplayName("GET top doc tu replica: thanh vien chi ton tai tren replica van hien ra")
    void apiDocTuReplicaChuKhongPhaiMaster() throws Exception {
        master.opsForZSet().add(key, "nguoi-tren-master", 100);
        doiReplicaBatKip();

        tren(replica, "CONFIG", "SET", "replica-read-only", "no");
        try {
            replica.opsForZSet().add(key, "chi-co-tren-replica", 9999);

            assertThat(master.opsForZSet().score(key, "chi-co-tren-replica")).isNull();

            mockMvc.perform(get("/leaderboard/top/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].username", is("chi-co-tren-replica")));
        } finally {
            replica.opsForZSet().remove(key, "chi-co-tren-replica");
            tren(replica, "CONFIG", "SET", "replica-read-only", "yes");
        }
    }
}
