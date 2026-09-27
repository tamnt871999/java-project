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

@SpringBootTest(properties = "app.cap.strategy=cp")
@AutoConfigureMockMvc
@DisplayName("Chien luoc CP - phia thieu so tu choi phuc vu de giu nhat quan")
class CpStrategyApiTest {

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
                .andExpect(jsonPath("$.strategy").value("cp"))
                .andExpect(jsonPath("$.partitioned").value(false));
    }

    private void giuCho(String nodeId, int soLuong, int httpStatus) throws Exception {
        mockMvc.perform(post("/api/stock/{sku}/reserve", "TSHIRT-M")
                        .header("X-Node", nodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":" + soLuong + "}"))
                .andExpect(status().is(httpStatus));
    }

    @Test
    @DisplayName("luc mang binh thuong thi moi node deu giu cho duoc")
    void mangBinhThuongThiGiuChoDuoc() throws Exception {
        giuCho(MINORITY, 3, 200);

        mockMvc.perform(get("/api/stock/{sku}", "TSHIRT-M").header("X-Node", MAJORITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(2));
    }

    @Test
    @DisplayName("partition: phia da so van ghi duoc")
    void phiaDaSoVanGhiDuoc() throws Exception {
        mockMvc.perform(post("/api/cluster/partition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partitioned").value(true));

        giuCho(MAJORITY, 3, 200);

        mockMvc.perform(get("/api/stock/{sku}", "TSHIRT-M").header("X-Node", MAJORITY))
                .andExpect(jsonPath("$.available").value(2));
    }

    @Test
    @DisplayName("partition: phia thieu so tra 503 - day chinh la cai gia cua C")
    void phiaThieuSoTra503() throws Exception {
        mockMvc.perform(post("/api/cluster/partition")).andExpect(status().isOk());

        mockMvc.perform(post("/api/stock/{sku}/reserve", "TSHIRT-M")
                        .header("X-Node", MINORITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CLUSTER_UNAVAILABLE"));

        mockMvc.perform(get("/api/stock/{sku}", "TSHIRT-M").header("X-Node", MINORITY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CLUSTER_UNAVAILABLE"));
    }

    @Test
    @DisplayName("noi lai mang: khong co don nao bi ban vuot ton kho")
    void noiLaiMangKhongOversell() throws Exception {
        mockMvc.perform(post("/api/cluster/partition")).andExpect(status().isOk());

        giuCho(MAJORITY, 3, 200);
        giuCho(MINORITY, 3, 503);
        giuCho(MAJORITY, 2, 200);

        mockMvc.perform(post("/api/cluster/heal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.partitioned").value(false))
                .andExpect(jsonPath("$.reconciliations[0].sku").value("TSHIRT-M"))
                .andExpect(jsonPath("$.reconciliations[0].totalReserved").value(5))
                .andExpect(jsonPath("$.reconciliations[0].mergedAvailable").value(0))
                .andExpect(jsonPath("$.reconciliations[0].oversold").value(0));

        mockMvc.perform(get("/api/stock/{sku}", "TSHIRT-M").header("X-Node", MINORITY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(0));
    }

    @Test
    @DisplayName("het hang tra 422, khong phai 503")
    void hetHangTra422() throws Exception {
        giuCho(MAJORITY, 5, 200);

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
    @DisplayName("thieu quantity tra 400, node la tra 400")
    void dauVaoSaiCuPhapTra400() throws Exception {
        mockMvc.perform(post("/api/stock/{sku}/reserve", "TSHIRT-M")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(get("/api/stock/{sku}", "TSHIRT-M").header("X-Node", "node-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
