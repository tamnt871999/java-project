package com.example.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.cap.strategy=ap")
@AutoConfigureMockMvc
@DisplayName("Chien luoc AP - luon phuc vu, tra gia bang oversell luc hoa giai")
class ApStrategyApiTest {

    private static final String MAJORITY = "node-1";
    private static final String MINORITY = "node-3";

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void datLaiCluster() throws Exception {
        mockMvc.perform(post("/api/cluster/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"TSHIRT-M\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("ap"));
    }

    private void giuCho(String nodeId, int soLuong, int conLai) throws Exception {
        mockMvc.perform(post("/api/stock/{sku}/reserve", "TSHIRT-M")
                        .header("X-Node", nodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":" + soLuong + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(conLai));
    }

    @Test
    @DisplayName("partition: ca hai phia deu nhan don - khong phia nao tra 503")
    void caHaiPhiaDeuNhanDon() throws Exception {
        mockMvc.perform(post("/api/cluster/partition"))
                .andExpect(jsonPath("$.partitioned").value(true));

        giuCho(MAJORITY, 3, 2);
        giuCho(MINORITY, 3, 2);
    }

    @Test
    @DisplayName("partition: hai phia doc ra hai gia tri khac nhau cua cung mot SKU")
    void haiPhiaDocRaHaiGiaTriKhacNhau() throws Exception {
        mockMvc.perform(post("/api/cluster/partition")).andExpect(status().isOk());

        giuCho(MAJORITY, 4, 1);

        mockMvc.perform(get("/api/stock/{sku}", "TSHIRT-M").header("X-Node", MAJORITY))
                .andExpect(jsonPath("$.available").value(1));

        mockMvc.perform(get("/api/stock/{sku}", "TSHIRT-M").header("X-Node", MINORITY))
                .andExpect(jsonPath("$.available").value(5));
    }

    @Test
    @DisplayName("noi lai mang: gop hai phia ra ton kho am - da ban 6 cai trong khi chi co 5")
    void noiLaiMangSinhRaOversell() throws Exception {
        mockMvc.perform(post("/api/cluster/partition")).andExpect(status().isOk());

        giuCho(MAJORITY, 3, 2);
        giuCho(MINORITY, 3, 2);

        mockMvc.perform(post("/api/cluster/heal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.partitioned").value(false))
                .andExpect(jsonPath("$.reconciliations[0].sku").value("TSHIRT-M"))
                .andExpect(jsonPath("$.reconciliations[0].initial").value(5))
                .andExpect(jsonPath("$.reconciliations[0].totalReserved").value(6))
                .andExpect(jsonPath("$.reconciliations[0].mergedAvailable").value(-1))
                .andExpect(jsonPath("$.reconciliations[0].oversold").value(1));
    }

    @Test
    @DisplayName("sau hoa giai, ton kho luu lai duoc chan ve 0 - phan thieu phai bu bang nghiep vu")
    void sauHoaGiaiTonKhoChanVeKhong() throws Exception {
        mockMvc.perform(post("/api/cluster/partition")).andExpect(status().isOk());

        giuCho(MAJORITY, 3, 2);
        giuCho(MINORITY, 3, 2);

        mockMvc.perform(post("/api/cluster/heal")).andExpect(status().isOk());

        mockMvc.perform(get("/api/stock/{sku}", "TSHIRT-M").header("X-Node", MINORITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(0));
    }

    @Test
    @DisplayName("bat bien cua domain van chan tren tung node - khong con hang thi 422")
    void batBienVanChanTrenTungNode() throws Exception {
        giuCho(MAJORITY, 5, 0);

        mockMvc.perform(post("/api/stock/{sku}/reserve", "TSHIRT-M")
                        .header("X-Node", MAJORITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATED"));
    }
}
