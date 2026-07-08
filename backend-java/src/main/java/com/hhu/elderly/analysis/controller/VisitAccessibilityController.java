package com.hhu.elderly.analysis.controller;

import com.hhu.elderly.analysis.dto.VisitAccessibilityResponse;
import com.hhu.elderly.analysis.service.VisitAccessibilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
public class VisitAccessibilityController {

    private final VisitAccessibilityService visitAccessibilityService;

    public VisitAccessibilityController(VisitAccessibilityService visitAccessibilityService) {
        this.visitAccessibilityService = visitAccessibilityService;
    }

    @GetMapping("/visit-accessibility")
    public VisitAccessibilityResponse analyzeVisitAccessibility(
            @RequestParam Double lon,
            @RequestParam Double lat,
            @RequestParam(required = false) String startName,
            @RequestParam(defaultValue = "driving") String mode
    ) {
        return visitAccessibilityService.analyze(lon, lat, startName, mode);
    }

    @GetMapping("/visit-accessibility/ping")
    public String ping() {
        return "visit accessibility controller ok";
    }
}