package com.example.inventory;

import com.example.inventory.cluster.ApStrategy;
import com.example.inventory.cluster.CapStrategies;
import com.example.inventory.cluster.ClusterNodes;
import com.example.inventory.cluster.CpStrategy;
import com.example.inventory.exception.BusinessRuleException;
import com.example.inventory.exception.ClusterUnavailableException;
import com.example.inventory.exception.SkuNotFoundException;
import com.example.inventory.service.StockService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StockService - chay bang new, khong can Spring context")
class StockServiceTest {

    private static final String SKU = "TSHIRT-M";
    private static final String MAJORITY = "node-1";
    private static final String MINORITY = "node-3";

    private ClusterNodes nodes;
    private StockService stockService;

    @BeforeEach
    void dungCum() {
        nodes = new ClusterNodes();
        CapStrategies strategies = new CapStrategies(
                List.of(new CpStrategy(nodes), new ApStrategy(nodes)), "cp");
        stockService = new StockService(nodes, strategies);
        nodes.reset(Map.of(SKU, 5));
    }

    @Test
    @DisplayName("giu cho hop le thi ton kho giam dung")
    void giuChoHopLe() {
        StockService.Reservation kq = stockService.reserve("cp", MAJORITY, SKU, 3);

        assertThat(kq.reserved()).isEqualTo(3);
        assertThat(kq.available()).isEqualTo(2);
        assertThat(kq.servedBy()).isEqualTo(MAJORITY);
    }

    @Test
    @DisplayName("khong giu cho qua ton kho con lai")
    void khongGiuChoQuaTonKho() {
        assertThatThrownBy(() -> stockService.reserve("cp", MAJORITY, SKU, 6))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ton kho khong du");

        assertThat(stockService.getStock("cp", MAJORITY, SKU).available()).isEqualTo(5);
    }

    @Test
    @DisplayName("so luong khong duong bi chan")
    void soLuongKhongDuongBiChan() {
        assertThatThrownBy(() -> stockService.reserve("cp", MAJORITY, SKU, 0))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> stockService.reserve("cp", MAJORITY, SKU, -1))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(stockService.getStock("cp", MAJORITY, SKU).available()).isEqualTo(5);
    }

    @Test
    @DisplayName("SKU khong ton tai")
    void skuKhongTonTai() {
        assertThatThrownBy(() -> stockService.getStock("cp", MAJORITY, "KHONG-CO"))
                .isInstanceOf(SkuNotFoundException.class);
    }

    @Test
    @DisplayName("node khong hop le bi chan ngay")
    void nodeKhongHopLe() {
        assertThatThrownBy(() -> stockService.getStock("cp", "node-99", SKU))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Nested
    @DisplayName("Sau khi mang bi cat")
    class SauKhiCatMang {

        @BeforeEach
        void catMang() {
            nodes.setPartitioned(true);
        }

        @Test
        @DisplayName("CP: phia da so van ghi duoc")
        void cpPhiaDaSoVanGhiDuoc() {
            assertThat(stockService.reserve("cp", MAJORITY, SKU, 3).available()).isEqualTo(2);
        }

        @Test
        @DisplayName("CP: phia thieu so tu choi ca doc lan ghi")
        void cpPhiaThieuSoTuChoi() {
            assertThatThrownBy(() -> stockService.reserve("cp", MINORITY, SKU, 1))
                    .isInstanceOf(ClusterUnavailableException.class);
            assertThatThrownBy(() -> stockService.getStock("cp", MINORITY, SKU))
                    .isInstanceOf(ClusterUnavailableException.class);
        }

        @Test
        @DisplayName("AP: ca hai phia deu nhan don va deu tin la con 2")
        void apCaHaiPhiaDeuNhan() {
            assertThat(stockService.reserve("ap", MAJORITY, SKU, 3).available()).isEqualTo(2);
            assertThat(stockService.reserve("ap", MINORITY, SKU, 3).available()).isEqualTo(2);
        }
    }
}
