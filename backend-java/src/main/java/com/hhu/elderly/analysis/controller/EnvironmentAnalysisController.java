package com.hhu.elderly.analysis.controller;

import com.hhu.elderly.analysis.dto.EnvironmentAnalysisResponse;
import com.hhu.elderly.analysis.service.EnvironmentAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/analysis")
@RestController
public class EnvironmentAnalysisController {

    private final EnvironmentAnalysisService environmentAnalysisService;

    public EnvironmentAnalysisController(EnvironmentAnalysisService environmentAnalysisService) {
        this.environmentAnalysisService = environmentAnalysisService;
    }

    @GetMapping("/environment/{institutionId}")
    public EnvironmentAnalysisResponse analyzeEnvironment(
            @PathVariable Long institutionId
    ) {
        return environmentAnalysisService.analyze(institutionId);
    }
}