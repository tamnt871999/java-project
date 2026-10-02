package com.example.inventory.controller;

import com.example.inventory.service.CapDemoService;
import com.example.inventory.service.CapDemoService.DemoResult;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class CapDemoController {

    private final CapDemoService capDemoService;

    public CapDemoController(CapDemoService capDemoService) {
        this.capDemoService = capDemoService;
    }

    @PostMapping("/cap")
    public DemoResult runCapScenario(
            @RequestParam(value = "strategy", required = false) String strategy) {
        return capDemoService.run(strategy);
    }
}
