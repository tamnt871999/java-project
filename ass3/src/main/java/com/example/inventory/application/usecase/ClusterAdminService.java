package com.example.inventory.application.usecase;

import com.example.inventory.application.port.in.ClusterAdminUseCase;
import com.example.inventory.application.port.out.ClusterControl;
import com.example.inventory.application.port.out.ClusterControl.ClusterState;
import com.example.inventory.application.port.out.ClusterControl.HealReport;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
class ClusterAdminService implements ClusterAdminUseCase {

    private final ClusterControl clusterControl;

    ClusterAdminService(ClusterControl clusterControl) {
        this.clusterControl = clusterControl;
    }

    @Override
    public ClusterState state() {
        return clusterControl.state();
    }

    @Override
    public ClusterState partition() {
        return clusterControl.partition();
    }

    @Override
    public HealReport heal() {
        return clusterControl.heal();
    }

    @Override
    public ClusterState reset(Map<String, Integer> seed) {
        return clusterControl.reset(seed);
    }
}
