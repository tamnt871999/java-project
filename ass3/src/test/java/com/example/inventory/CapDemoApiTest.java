package com.example.inventory;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Kich ban CAP co tuong thuat tung buoc")
class CapDemoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("CP: phia thieu so bi tu choi, ket thuc khong oversell")
    void kichBanCp() throws Exception {
        mockMvc.perform(post("/api/demo/cap").param("strategy", "cp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("cp"))
                .andExpect(jsonPath("$.steps.length()").value(7))
                .andExpect(jsonPath("$.steps[2].result").value(Matchers.startsWith("CHAP NHAN")))
                .andExpect(jsonPath("$.steps[3].result").value(Matchers.startsWith("TU CHOI")))
                .andExpect(jsonPath("$.steps[5].result").value(Matchers.startsWith("TU CHOI")))
                .andExpect(jsonPath("$.reconciliation.totalReserved").value(3))
                .andExpect(jsonPath("$.reconciliation.mergedAvailable").value(2))
                .andExpect(jsonPath("$.reconciliation.oversold").value(0));
    }

    @Test
    @DisplayName("AP: khong ai bi tu choi, nhung ket thuc oversell 1 cai")
    void kichBanAp() throws Exception {
        mockMvc.perform(post("/api/demo/cap").param("strategy", "ap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("ap"))
                .andExpect(jsonPath("$.steps.length()").value(7))
                .andExpect(jsonPath("$.steps[2].result").value(Matchers.startsWith("CHAP NHAN")))
                .andExpect(jsonPath("$.steps[3].result").value(Matchers.startsWith("CHAP NHAN")))
                .andExpect(jsonPath("$.steps[4].majorityNodeSees").value(2))
                .andExpect(jsonPath("$.steps[4].minorityNodeSees").value(2))
                .andExpect(jsonPath("$.reconciliation.totalReserved").value(6))
                .andExpect(jsonPath("$.reconciliation.mergedAvailable").value(-1))
                .andExpect(jsonPath("$.reconciliation.oversold").value(1));
    }

    @Test
    @DisplayName("moi buoc deu co loi giai thich, khong de trong")
    void moiBuocDeuCoGiaiThich() throws Exception {
        mockMvc.perform(post("/api/demo/cap").param("strategy", "cp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[*].explanation")
                        .value(Matchers.everyItem(Matchers.not(Matchers.emptyString()))))
                .andExpect(jsonPath("$.conclusion").value(Matchers.not(Matchers.emptyString())));
    }

    @Test
    @DisplayName("chien luoc la tra 400")
    void chienLuocLaTra400() throws Exception {
        mockMvc.perform(post("/api/demo/cap").param("strategy", "khong-co"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
