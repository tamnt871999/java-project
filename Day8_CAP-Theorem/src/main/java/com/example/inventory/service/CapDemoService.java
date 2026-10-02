package com.example.inventory.service;

import com.example.inventory.cluster.CapStrategies;
import com.example.inventory.cluster.CapStrategy;
import com.example.inventory.cluster.ClusterNodes;
import com.example.inventory.service.ClusterService.HealReport;
import com.example.inventory.service.ClusterService.Reconciliation;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CapDemoService {

    private static final String SKU = "TSHIRT-M";
    private static final int TON_KHO = 5;
    private static final int MOI_BEN_GIU = 3;

    private final ClusterNodes nodes;
    private final ClusterService clusterService;
    private final StockService stockService;
    private final CapStrategies strategies;

    public CapDemoService(ClusterNodes nodes, ClusterService clusterService,
                          StockService stockService, CapStrategies strategies) {
        this.nodes = nodes;
        this.clusterService = clusterService;
        this.stockService = stockService;
        this.strategies = strategies;
    }

    public DemoResult run(String strategyName) {
        CapStrategy strategy = strategies.resolve(strategyName);
        String majority = nodes.majorityNode();
        String minority = nodes.minorityNode();
        List<Step> steps = new ArrayList<>();

        clusterService.reset(Map.of(SKU, TON_KHO));
        steps.add(step(steps, "Dat lai cum: ton kho " + SKU + " = " + TON_KHO,
                "OK",
                "Ba node deu thay " + TON_KHO + ". Mang con lanh lan, chua co gi de tranh cai."));

        clusterService.partition();
        steps.add(step(steps, "Cat mang: " + minority + " bi co lap khoi " + majority,
                "OK",
                "Day la chu P. Khong ai CHON partition ca - mang tu dut. Tu gio he thong "
                        + "buoc phai chon: tiep tuc phuc vu (A) hay giu nhat quan (C)."));

        steps.add(giuCho(steps, strategy.name(), majority, "Phia DA SO (" + majority + ") giu "
                + MOI_BEN_GIU + " san pham"));

        steps.add(giuCho(steps, strategy.name(), minority, "Phia THIEU SO (" + minority + ") giu "
                + MOI_BEN_GIU + " san pham"));

        steps.add(doc(steps, strategy.name(), majority, "Khach doc ton kho tren " + majority));
        steps.add(doc(steps, strategy.name(), minority, "Khach doc ton kho tren " + minority));

        HealReport report = clusterService.heal(strategy.name());
        Reconciliation line = report.reconciliations().get(0);
        steps.add(step(steps, "Noi lai mang va hoa giai",
                "da ban " + line.totalReserved() + "/" + line.initial()
                        + ", oversold = " + line.oversold(),
                giaiThichHoaGiai(strategy.name(), line)));

        return new DemoResult(strategy.name(), strategy.summary(), SKU, TON_KHO,
                steps, line, ketLuan(strategy.name(), line));
    }

    private Step giuCho(List<Step> steps, String strategyName, String nodeId, String hanhDong) {
        try {
            StockService.Reservation kq = stockService.reserve(strategyName, nodeId, SKU, MOI_BEN_GIU);
            return step(steps, hanhDong, "CHAP NHAN, node nay con " + kq.available(),
                    "Node " + nodeId + " tu kiem tra ban sao CUA RIENG NO va thay du hang. "
                            + "No khong biet phia ben kia vua lam gi.");
        } catch (RuntimeException e) {
            return step(steps, hanhDong, "TU CHOI - " + e.getClass().getSimpleName(),
                    "Node " + nodeId + " khong lien lac du quorum nen tu choi thay vi doan. "
                            + "Khach nhan 503 va mat co hoi mua - do chinh la cai gia cua C.");
        }
    }

    private Step doc(List<Step> steps, String strategyName, String nodeId, String hanhDong) {
        try {
            StockService.StockView view = stockService.getStock(strategyName, nodeId, SKU);
            return step(steps, hanhDong, "tra ve " + view.available(),
                    "Day la thu " + nodeId + " TIN la dung tai thoi diem nay.");
        } catch (RuntimeException e) {
            return step(steps, hanhDong, "TU CHOI - " + e.getClass().getSimpleName(),
                    "Cho doc du lieu cu cung la vi pham C, nen chien luoc CP chan luon ca doc.");
        }
    }

    private String giaiThichHoaGiai(String strategyName, Reconciliation line) {
        if ("cp".equals(strategyName)) {
            return "Phia thieu so chua he ghi duoc gi nen khong co gi de gop. Node " + nodes.minorityNode()
                    + " chi viec chep lai trang thai cua phia da so. Khong co conflict.";
        }
        return "Gop hai phia: " + line.initial() + " - " + MOI_BEN_GIU + " - " + MOI_BEN_GIU
                + " = " + line.mergedAvailable() + ". Phep gop nay KHONG SAI - no cong du moi "
                + "thao tac, khong mat don nao. Nhung ket qua am nghia la da ban "
                + line.totalReserved() + " cai trong khi chi co " + line.initial() + ".";
    }

    private String ketLuan(String strategyName, Reconciliation line) {
        if ("cp".equals(strategyName)) {
            return "CP: ban duoc " + line.totalReserved() + "/" + line.initial()
                    + ", KHONG oversell. Cai gia phai tra la phia thieu so ngung phuc vu hoan toan "
                    + "trong suot thoi gian partition - khach o phia do khong mua duoc gi.";
        }
        return "AP: khong mot khach nao bi tu choi, nhung da ban " + line.totalReserved()
                + " cai trong khi chi co " + line.initial() + " (oversold = " + line.oversold()
                + "). Bat bien chi dung tren tung ban sao, khong dung tren ca he thong. "
                + "Phan du phai bu bang nghiep vu: huy don, hoan tien, tang voucher.";
    }

    private Step step(List<Step> steps, String action, String result, String explanation) {
        return new Step(steps.size() + 1, action, result, explanation,
                nodes.availableOn(nodes.majorityNode(), SKU).orElse(null),
                nodes.availableOn(nodes.minorityNode(), SKU).orElse(null));
    }

    public record Step(int step, String action, String result, String explanation,
                       Integer majorityNodeSees, Integer minorityNodeSees) {
    }

    public record DemoResult(String strategy, String summary, String sku, int initialStock,
                             List<Step> steps, Reconciliation reconciliation, String conclusion) {
    }
}
