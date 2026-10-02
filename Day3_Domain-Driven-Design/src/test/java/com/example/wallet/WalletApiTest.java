package com.example.wallet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("REST API - chay qua ca ba tang")
class WalletApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String moVi(String soDuBanDau) throws Exception {
        String body = mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initialBalance\":" + soDuBanDau + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        return json.get("walletId").asText();
    }

    @Test
    @DisplayName("mo vi roi rut tien - so du giam va luu xuong DB")
    void moViRoiRutTien() throws Exception {
        String walletId = moVi("1000.00");

        mockMvc.perform(post("/api/wallets/{id}/withdraw", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":300.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(700.00));

        mockMvc.perform(get("/api/wallets/{id}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(700.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("rut qua so du tra 422 va so du khong doi")
    void rutQuaSoDuTra422() throws Exception {
        String walletId = moVi("1000.00");

        mockMvc.perform(post("/api/wallets/{id}/withdraw", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":5000.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATED"));

        mockMvc.perform(get("/api/wallets/{id}", walletId))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    @DisplayName("khoa vi roi rut tien tra 422")
    void khoaViRoiRutTienTra422() throws Exception {
        String walletId = moVi("1000.00");

        mockMvc.perform(post("/api/wallets/{id}/lock", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));

        mockMvc.perform(post("/api/wallets/{id}/withdraw", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATED"));
    }

    @Test
    @DisplayName("rut so am tra 422")
    void rutSoAmTra422() throws Exception {
        String walletId = moVi("1000.00");

        mockMvc.perform(post("/api/wallets/{id}/withdraw", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-100.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATED"));
    }

    @Test
    @DisplayName("vi khong ton tai tra 404")
    void viKhongTonTaiTra404() throws Exception {
        mockMvc.perform(get("/api/wallets/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WALLET_NOT_FOUND"));
    }

    @Test
    @DisplayName("thieu truong amount tra 400")
    void thieuAmountTra400() throws Exception {
        String walletId = moVi("1000.00");

        mockMvc.perform(post("/api/wallets/{id}/withdraw", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
