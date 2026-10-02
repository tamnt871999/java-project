package com.example.leaderboard;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.leaderboard.key=leaderboard:test")
@AutoConfigureMockMvc
class LeaderboardApiTest extends RedisTestBase {

    @Autowired
    private MockMvc mockMvc;

    private void ghiDiem(String username, int score) throws Exception {
        mockMvc.perform(post("/leaderboard/" + username)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\": " + score + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(username)))
                .andExpect(jsonPath("$.score", is((double) score)));
    }

    @Test
    @DisplayName("Ghi diem roi lay top thi thay dung thu tu giam dan")
    void ghiDiemRoiLayTopThayDungThuTu() throws Exception {
        ghiDiem("an", 1500);
        ghiDiem("binh", 2700);
        ghiDiem("cuong", 900);
        doiReplicaBatKip();

        mockMvc.perform(get("/leaderboard/top/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].username", is("binh")))
                .andExpect(jsonPath("$[0].score", is(2700.0)))
                .andExpect(jsonPath("$[1].username", is("an")))
                .andExpect(jsonPath("$[2].username", is("cuong")));
    }

    @Test
    @DisplayName("Top N chi tra ve dung N nguoi du bang co nhieu hon")
    void topNChiTraVeDungNPhanTu() throws Exception {
        ghiDiem("an", 100);
        ghiDiem("binh", 200);
        ghiDiem("cuong", 300);
        ghiDiem("dung", 400);
        ghiDiem("em", 500);
        doiReplicaBatKip();

        mockMvc.perform(get("/leaderboard/top/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username", is("em")))
                .andExpect(jsonPath("$[1].username", is("dung")));
    }

    @Test
    @DisplayName("ZADD lai cung mot nguoi thi ghi de diem chu khong cong don")
    void ghiLaiDiemThiGhiDeChuKhongCongDon() throws Exception {
        ghiDiem("an", 1000);
        ghiDiem("an", 300);
        doiReplicaBatKip();

        mockMvc.perform(get("/leaderboard/top/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].score", is(300.0)));
    }

    @Test
    @DisplayName("Bang xep hang rong thi tra ve mang rong chu khong phai loi")
    void bangRongTraVeMangRong() throws Exception {
        mockMvc.perform(get("/leaderboard/top/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Thieu truong score tra 400")
    void thieuTruongScoreTra400() throws Exception {
        mockMvc.perform(post("/leaderboard/an")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }

    @Test
    @DisplayName("Body khong phai JSON hop le tra 400")
    void bodyHongTra400() throws Exception {
        mockMvc.perform(post("/leaderboard/an")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\": }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }

    @Test
    @DisplayName("N khong phai so nguyen tra 400")
    void nKhongPhaiSoTra400() throws Exception {
        mockMvc.perform(get("/leaderboard/top/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }

    @Test
    @DisplayName("N bang 0 la cu phap dung nhung nghiep vu tu choi, tra 422")
    void nBangKhongTra422() throws Exception {
        mockMvc.perform(get("/leaderboard/top/0"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("BUSINESS_RULE_VIOLATED")));
    }

    @Test
    @DisplayName("Ten nguoi choi chi co khoang trang tra 422")
    void tenNguoiChoiTrongTra422() throws Exception {
        mockMvc.perform(post(URI.create("/leaderboard/%20"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\": 10}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("BUSINESS_RULE_VIOLATED")));
    }
}
