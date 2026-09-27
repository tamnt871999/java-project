package com.example.inventory.application.port.in;

import com.example.inventory.application.port.out.ClusterControl.ClusterState;
import com.example.inventory.application.port.out.ClusterControl.HealReport;

import java.util.Map;

public interface ClusterAdminUseCase {

    ClusterState state();

    ClusterState partition();

    HealReport heal();

    ClusterState reset(Map<String, Integer> seed);
}
