package com.example.inventory.controller;

import com.example.inventory.service.ClusterService;
import com.example.inventory.service.ClusterService.ClusterState;
import com.example.inventory.service.ClusterService.HealReport;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    private final ClusterService clusterService;

    public ClusterController(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    @GetMapping
    public ClusterState state() {
        return clusterService.state();
    }

    @PostMapping("/partition")
    public ClusterState partition() {
        return clusterService.partition();
    }

    @PostMapping("/heal")
    public HealReport heal(@RequestHeader(value = "X-Strategy", required = false) String strategy) {
        return clusterService.heal(strategy);
    }

    @PostMapping("/reset")
    public ClusterState reset(@RequestBody(required = false) Map<String, Integer> seed) {
        return clusterService.reset(seed);
    }
}
