package com.example.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.leaderboard.repository.ProductRepository;
import com.example.leaderboard.service.ProductDeletionListener;
import com.example.leaderboard.service.ProductLocalCache;
import com.example.leaderboard.service.ProductService;
import com.example.leaderboard.service.ProductService.ProductView;
import java.math.BigDecimal;
import java.util.function.BooleanSupplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProductDeletionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository repository;

    @Autowired
    private ProductLocalCache localCache;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private DataSource dataSource;

    @Autowired
    @Qualifier("masterConnectionFactory")
    private RedisConnectionFactory masterFactory;

    @Value("${app.product.deletion-channel}")
    private String channel;

    @BeforeEach
    void napLaiSanPhamVaLamRongHaiTangCache() {
        new ResourceDatabasePopulator(new ClassPathResource("data.sql")).execute(dataSource);
        l2().clear();
        for (long id = 1; id <= 3; id++) {
            localCache.evict(id);
        }
    }

    private Cache l2() {
        return cacheManager.getCache(ProductService.CACHE);
    }

    @Test
    @DisplayName("Nen: doc lan dau thi san pham duoc nap vao ca L1 lan L2")
    void docLanDauNapVaoCaL1LanL2() throws Exception {
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Ban phim co")));

        assertThat(localCache.get(1L)).isPresent();
        assertThat(l2().get(1L)).isNotNull();
    }

    @Test
    @DisplayName("Y 1: DELETE /products/{id} tra 204")
    void xoaTra204() throws Exception {
        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Y 2: xoa thi san pham mat khoi DB")
    void xoaThiSanPhamMatKhoiDb() throws Exception {
        assertThat(repository.existsById(1L)).isTrue();

        mockMvc.perform(delete("/products/1")).andExpect(status().isNoContent());

        assertThat(repository.existsById(1L)).isFalse();
    }

    @Test
    @DisplayName("Y 3: xoa thi @CacheEvict go san pham khoi L2")
    void xoaThiSanPhamMatKhoiL2() throws Exception {
        mockMvc.perform(get("/products/1")).andExpect(status().isOk());
        assertThat(l2().get(1L)).isNotNull();

        mockMvc.perform(delete("/products/1")).andExpect(status().isNoContent());

        assertThat(l2().get(1L)).isNull();
    }

    @Test
    @DisplayName("Y 4: xoa thi instance khac nhan thong bao qua Pub/Sub va go san pham khoi L1 cua no")
    void xoaGuiThongBaoChoInstanceKhacXoaL1() throws Exception {
        ProductLocalCache l1CuaInstanceKhac = new ProductLocalCache();
        l1CuaInstanceKhac.put(new ProductView(2L, "Chuot khong day", new BigDecimal("450000.00")));

        RedisMessageListenerContainer instanceKhac = new RedisMessageListenerContainer();
        instanceKhac.setConnectionFactory(masterFactory);
        instanceKhac.addMessageListener(
                new ProductDeletionListener(l1CuaInstanceKhac), new ChannelTopic(channel));
        instanceKhac.afterPropertiesSet();
        instanceKhac.start();
        try {
            doiDenKhi(instanceKhac::isListening, "Instance khac chua kip subscribe channel");

            mockMvc.perform(delete("/products/2")).andExpect(status().isNoContent());

            doiDenKhi(() -> l1CuaInstanceKhac.get(2L).isEmpty(),
                    "Instance khac khong nhan duoc thong bao xoa san pham 2");
        } finally {
            instanceKhac.stop();
            instanceKhac.destroy();
        }
    }

    @Test
    @DisplayName("Instance dang xu ly DELETE tu go san pham khoi L1 cua chinh no, khong cho Pub/Sub")
    void instanceXuLyXoaTuGoL1CuaMinh() throws Exception {
        mockMvc.perform(get("/products/1")).andExpect(status().isOk());
        assertThat(localCache.get(1L)).isPresent();

        mockMvc.perform(delete("/products/1")).andExpect(status().isNoContent());

        assertThat(localCache.get(1L)).isEmpty();
    }

    @Test
    @DisplayName("Sau khi xoa, doc lai qua ca ba tang L1 -> L2 -> DB deu khong con, tra 404")
    void sauKhiXoaDocLaiTra404() throws Exception {
        mockMvc.perform(get("/products/1")).andExpect(status().isOk());

        mockMvc.perform(delete("/products/1")).andExpect(status().isNoContent());

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PRODUCT_NOT_FOUND")));
    }

    @Test
    @DisplayName("Xoa san pham khong ton tai van tra 204 vi DELETE la idempotent")
    void xoaSanPhamKhongTonTaiVanTra204() throws Exception {
        mockMvc.perform(delete("/products/999"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Id khong phai so nguyen tra 400")
    void idKhongPhaiSoTra400() throws Exception {
        mockMvc.perform(delete("/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")));
    }

    private static void doiDenKhi(BooleanSupplier dieuKien, String loi) throws InterruptedException {
        for (int lan = 0; lan < 150; lan++) {
            if (dieuKien.getAsBoolean()) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError(loi + " sau 3 giay");
    }
}
