package com.example.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.inventory.entity.Inventory;
import com.example.inventory.repository.InventoryRepository;
import com.example.inventory.service.InventoryService;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryApiTest {

    private static final long PRODUCT_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryRepository repository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void napLaiMotSanPhamVoiLichSuSach() {
        jdbc.update("delete from inventory_aud");
        jdbc.update("delete from revinfo");
        jdbc.update("delete from inventory");
        repository.save(new Inventory(PRODUCT_ID, "Ban phim co", 100));
    }

    @Test
    @DisplayName("Y 3: nhap hang thi quantity tang dung so luong nhap")
    void nhapHangTangSoLuong() throws Exception {
        mockMvc.perform(post("/inventories/1/stock-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(1)))
                .andExpect(jsonPath("$.quantity", is(105)))
                .andExpect(jsonPath("$.version", is(1)));
    }

    @Test
    @DisplayName("Y 3: 20 nguoi nhap hang cung luc thi khong mat luot nao va khong ai bi tu choi")
    void nhieuNguoiCungNhapHangKhongMatLuotNao() throws Exception {
        int nguoi = 20;
        ExecutorService pool = Executors.newFixedThreadPool(nguoi);
        CountDownLatch xuatPhat = new CountDownLatch(1);
        List<Future<?>> ketQua = new ArrayList<>();
        try {
            for (int i = 0; i < nguoi; i++) {
                ketQua.add(pool.submit(() -> {
                    xuatPhat.await();
                    return inventoryService.stockIn(PRODUCT_ID, 1);
                }));
            }
            xuatPhat.countDown();
            for (Future<?> lanNhap : ketQua) {
                lanNhap.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(repository.findById(PRODUCT_ID).orElseThrow().getQuantity()).isEqualTo(100 + nguoi);
    }

    @Test
    @DisplayName("Y 3: dang co nguoi giu khoa thi lan nhap hang khac phai cho, nha khoa xong moi doc so luong moi")
    void nhapHangPhaiChoKhiDongDangBiKhoa() throws Exception {
        CountDownLatch daKhoa = new CountDownLatch(1);
        CountDownLatch nhaKhoa = new CountDownLatch(1);
        CompletableFuture<Void> nguoiGiuKhoa = CompletableFuture.runAsync(() -> transaction.executeWithoutResult(status -> {
            repository.findByIdForUpdate(PRODUCT_ID).orElseThrow().stockIn(10);
            daKhoa.countDown();
            try {
                nhaKhoa.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        assertThat(daKhoa.await(5, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<InventoryService.InventoryView> nguoiNhapHang =
                CompletableFuture.supplyAsync(() -> inventoryService.stockIn(PRODUCT_ID, 5));
        Thread.sleep(300);
        assertThat(nguoiNhapHang).isNotDone();

        nhaKhoa.countDown();
        nguoiGiuKhoa.get(5, TimeUnit.SECONDS);
        assertThat(nguoiNhapHang.get(5, TimeUnit.SECONDS).quantity()).isEqualTo(115);
    }

    @Test
    @DisplayName("Nhap so luong khong duong tra 400")
    void nhapSoLuongKhongDuongTra400() throws Exception {
        mockMvc.perform(post("/inventories/1/stock-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }

    @Test
    @DisplayName("Nhap hang cho san pham khong ton tai tra 404")
    void nhapHangSanPhamKhongTonTaiTra404() throws Exception {
        mockMvc.perform(post("/inventories/99/stock-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("INVENTORY_NOT_FOUND")));
    }

    @Test
    @DisplayName("Y 4: gui dung version dang co thi doi ten thanh cong va version tang")
    void capNhatDungVersionThanhCong() throws Exception {
        mockMvc.perform(put("/inventories/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\": \"Ban phim co Pro\", \"version\": 0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName", is("Ban phim co Pro")))
                .andExpect(jsonPath("$.quantity", is(100)))
                .andExpect(jsonPath("$.version", is(1)));
    }

    @Test
    @DisplayName("Y 4: gui version cu vi nguoi khac vua sua thi tra 409 va ten khong doi")
    void capNhatVersionCuTra409() throws Exception {
        mockMvc.perform(put("/inventories/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\": \"Nguoi A dat ten\", \"version\": 0}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/inventories/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\": \"Nguoi B dat ten\", \"version\": 0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("VERSION_CONFLICT")));

        assertThat(repository.findById(PRODUCT_ID).orElseThrow().getProductName()).isEqualTo("Nguoi A dat ten");
    }

    @Test
    @DisplayName("Y 4: hai giao dich cung doc mot version, giao dich ghi sau bi @Version tu choi")
    void haiGiaoDichCungSuaThiGiaoDichSauBiTuChoi() {
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            Inventory banDocCu = repository.findById(PRODUCT_ID).orElseThrow();
            CompletableFuture.runAsync(() -> inventoryService.update(PRODUCT_ID, "Nguoi A dat ten", 0)).join();
            banDocCu.rename("Nguoi B dat ten");
            repository.saveAndFlush(banDocCu);
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(repository.findById(PRODUCT_ID).orElseThrow().getProductName()).isEqualTo("Nguoi A dat ten");
    }

    @Test
    @DisplayName("Cap nhat thieu version hoac ten rong tra 400")
    void capNhatThieuVersionHoacTenRongTra400() throws Exception {
        mockMvc.perform(put("/inventories/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\": \"Ten moi\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/inventories/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\": \" \", \"version\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }

    @Test
    @DisplayName("Cap nhat san pham khong ton tai tra 404")
    void capNhatSanPhamKhongTonTaiTra404() throws Exception {
        mockMvc.perform(put("/inventories/99").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\": \"Ten moi\", \"version\": 0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("INVENTORY_NOT_FOUND")));
    }

    @Test
    @DisplayName("Y 5: Envers ghi lai moi thay doi cua Inventory, ke ca lan chi doi ten")
    void enversGhiLaiMoiThayDoi() throws Exception {
        inventoryService.stockIn(PRODUCT_ID, 5);
        inventoryService.update(PRODUCT_ID, "Ban phim co Pro", 1);

        var entityManager = entityManagerFactory.createEntityManager();
        try {
            var reader = AuditReaderFactory.get(entityManager);
            List<Number> revisions = reader.getRevisions(Inventory.class, PRODUCT_ID);
            assertThat(revisions).hasSize(3);
            Inventory banGhiCuoi = reader.find(Inventory.class, PRODUCT_ID, revisions.get(2));
            assertThat(banGhiCuoi.getProductName()).isEqualTo("Ban phim co Pro");
        } finally {
            entityManager.close();
        }
    }

    @Test
    @DisplayName("Y 6: lich su chi gom cac lan quantity thay doi, theo thu tu thoi gian")
    void lichSuChiGomCacLanDoiSoLuong() throws Exception {
        inventoryService.stockIn(PRODUCT_ID, 5);
        inventoryService.update(PRODUCT_ID, "Ban phim co Pro", 1);
        inventoryService.stockIn(PRODUCT_ID, 3);

        mockMvc.perform(get("/inventories/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].quantity", contains(100, 105, 108)))
                .andExpect(jsonPath("$[0].revision").isNumber())
                .andExpect(jsonPath("$[0].changedAt").isString());
    }

    @Test
    @DisplayName("Lich su cua san pham khong ton tai tra 404")
    void lichSuSanPhamKhongTonTaiTra404() throws Exception {
        mockMvc.perform(get("/inventories/99/history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("INVENTORY_NOT_FOUND")));
    }

    @Test
    @DisplayName("productId khong phai so nguyen tra 400")
    void productIdKhongPhaiSoTra400() throws Exception {
        mockMvc.perform(get("/inventories/abc/history"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }
}
