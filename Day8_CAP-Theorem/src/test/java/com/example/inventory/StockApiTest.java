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

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("REST API - chay qua ca ba tang")
class StockApiTest {

    private static final String MAJORITY = "node-1";
    private static final String MINORITY = "node-3";

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void datLaiCum() throws Exception {
        mockMvc.perform(post("/api/cluster/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"TSHIRT-M\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partitioned").value(false))
                .andExpect(jsonPath("$.quorum").value(2));
    }

    private void catMang() throws Exception {
        mockMvc.perform(post("/api/cluster/partition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partitioned").value(true));
    }

    @Test
    @DisplayName("mang binh thuong: giu cho roi doc lai thay so moi")
    void giuChoRoiDocLai() throws Exception {
        mockMvc.perform(post("/api/stock/{sku}/reserve", "TSHIRT-M")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(2))
                .andExpect(jsonPath("$.servedBy").value(MAJORITY))
                .andExpect(jsonPath("$.strategy").value("cp"));

        mockMvc.perform(get("/api/stock/{sku}", "TSHIRT-M").header("X-Node", MINORITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(2));
    }

    @Test
    @DisplayName("CP + partition: phia thieu so tra 503 cho ca doc lan ghi")
    void cpPhiaThieuSoTra503() throws Exception {
        catMang();

        mockMvc.perform(post("/api/stock/{sku}/reserve", "TSHIRT-M")
                        .header("X-Node", MINORITY)
                        .header("X-Strategy", "cp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CLUSTER_UNAVAILABLE"));

        mockMvc.perform(get("/api/stock/{sku}", "TSHIRT-M")
                        .header("X-Node", MINORITY)
                        .header("X-Strategy", "cp"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("CP + partition: phia da so van ghi binh thuong")
    void cpPhiaDaSoVanGhiDuoc() throws Exception {
        catMang();

        mockMvc.perform(post("/api/stock/{sku}/reserve", "TSHIRT-M")
                        .header("X-Node", MAJORITY)
                        .header("X-Strategy", "cp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(2));
    }

    @Test
    @DisplayName("AP + partition: ca hai phia deu nhan don, sau heal oversold = 1")
    void apSinhRaOversell() throws Exception {
        catMang();

        mockMvc.perform(post("/api/stock/{sku}/reserve", "TSHIRT-M")
                        .header("X-Node", MAJORITY)
                        .header("X-Strategy", "ap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(2));

        mockMvc.perform(post("/api/stock/{sku}/reserve", "TSHIRT-M")
                        .header("X-Node", MINORITY)
                        .header("X-Strategy", "ap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(2));

        mockMvc.perform(post("/api/cluster/heal").header("X-Strategy", "ap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciliations[0].totalReserved").value(6))
                .andExpect(jsonPath("$.reconciliations[0].mergedAvailable").value(-1))
                .andExpect(jsonPath("$.reconciliations[0].oversold").value(1));
    }

    @Test
    @DisplayName("het hang tra 422, khong phai 503")
    void hetHangTra422() throws Exception {
        mockMvc.perform(post("/api/stock/{sku}/reserve", "TSHIRT-M")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/stock/{sku}/reserve", "TSHIRT-M")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATED"));
    }

    @Test
    @DisplayName("sku khong ton tai tra 404")
    void skuKhongTonTaiTra404() throws Exception {
        mockMvc.perform(get("/api/stock/{sku}", "KHONG-CO"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SKU_NOT_FOUND"));
    }

    @Test
    @DisplayName("thieu quantity, node la, chien luoc la deu tra 400")
    void dauVaoSaiTra400() throws Exception {
        mockMvc.perform(post("/api/stock/{sku}/reserve", "TSHIRT-M")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/stock/{sku}", "TSHIRT-M").header("X-Node", "node-99"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/stock/{sku}", "TSHIRT-M").header("X-Strategy", "khong-co"))
                .andExpect(status().isBadRequest());
    }
}
