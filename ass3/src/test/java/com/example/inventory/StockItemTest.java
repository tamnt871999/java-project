package com.example.inventory;

import com.example.inventory.domain.DomainException;
import com.example.inventory.domain.StockItem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StockItem - bat bien ton kho tren mot node")
class StockItemTest {

    @Test
    @DisplayName("giu cho hop le thi ton kho giam dung")
    void giuChoHopLe() {
        StockItem item = StockItem.rehydrate("TSHIRT-M", 5);

        item.reserve(3);

        assertThat(item.available()).isEqualTo(2);
    }

    @Test
    @DisplayName("khong giu cho qua ton kho con lai")
    void khongGiuChoQuaTonKho() {
        StockItem item = StockItem.rehydrate("TSHIRT-M", 5);

        assertThatThrownBy(() -> item.reserve(6))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Ton kho khong du");

        assertThat(item.available()).isEqualTo(5);
    }

    @Test
    @DisplayName("so luong khong duong bi chan")
    void soLuongKhongDuongBiChan() {
        StockItem item = StockItem.rehydrate("TSHIRT-M", 5);

        assertThatThrownBy(() -> item.reserve(0)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> item.reserve(-1)).isInstanceOf(DomainException.class);

        assertThat(item.available()).isEqualTo(5);
    }

    @Test
    @DisplayName("bat bien chi dung tren mot ban sao - day la gioi han cua domain trong he phan tan")
    void batBienChiDungTrenMotBanSao() {
        StockItem nodeMajority = StockItem.rehydrate("TSHIRT-M", 5);
        StockItem nodeMinority = StockItem.rehydrate("TSHIRT-M", 5);

        nodeMajority.reserve(3);
        nodeMinority.reserve(3);

        assertThat(nodeMajority.available()).isEqualTo(2);
        assertThat(nodeMinority.available()).isEqualTo(2);

        int daBan = (5 - nodeMajority.available()) + (5 - nodeMinority.available());
        assertThat(daBan).isEqualTo(6);
        assertThat(daBan).isGreaterThan(5);
    }
}
