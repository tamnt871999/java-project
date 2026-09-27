package com.example.inventory.adapter.in.web;

import com.example.inventory.application.port.in.ClusterAdminUseCase;
import com.example.inventory.application.port.out.ClusterControl.ClusterState;
import com.example.inventory.application.port.out.ClusterControl.HealReport;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cluster")
class ClusterController {

    private final ClusterAdminUseCase clusterAdminUseCase;

    ClusterController(ClusterAdminUseCase clusterAdminUseCase) {
        this.clusterAdminUseCase = clusterAdminUseCase;
    }

    @GetMapping
    ClusterState state() {
        return clusterAdminUseCase.state();
    }

    @PostMapping("/partition")
    ClusterState partition() {
        return clusterAdminUseCase.partition();
    }

    @PostMapping("/heal")
    HealReport heal() {
        return clusterAdminUseCase.heal();
    }

    @PostMapping("/reset")
    ClusterState reset(@RequestBody(required = false) Map<String, Integer> seed) {
        return clusterAdminUseCase.reset(seed);
    }
}
