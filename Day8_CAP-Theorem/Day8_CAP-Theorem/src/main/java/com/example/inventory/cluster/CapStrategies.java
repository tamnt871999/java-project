package com.example.inventory.cluster;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CapStrategies {

    private final Map<String, CapStrategy> byName;
    private final String defaultName;

    public CapStrategies(List<CapStrategy> strategies,
                         @Value("${app.cap.strategy}") String defaultName) {
        this.byName = strategies.stream().collect(Collectors.toMap(
                CapStrategy::name, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        this.defaultName = defaultName;

        if (!byName.containsKey(defaultName)) {
            throw new IllegalStateException(
                    "app.cap.strategy = '" + defaultName + "' khong hop le. Chon: " + byName.keySet());
        }
    }

    public CapStrategy resolve(String name) {
        if (name == null || name.isBlank()) {
            return byName.get(defaultName);
        }
        CapStrategy strategy = byName.get(name.toLowerCase());
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Khong co chien luoc '" + name + "'. Chon: " + byName.keySet());
        }
        return strategy;
    }

    public String defaultName() {
        return defaultName;
    }
}
