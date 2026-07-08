package com.hhu.elderly.analysis.controller;

import com.hhu.elderly.analysis.dto.MedicalLifelineResponse;
import com.hhu.elderly.analysis.service.MedicalLifelineService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/analysis")
@RestController
public class MedicalLifelineController {

    private final MedicalLifelineService medicalLifelineService;

    public MedicalLifelineController(MedicalLifelineService medicalLifelineService) {
        this.medicalLifelineService = medicalLifelineService;
    }

    @GetMapping("/medical-lifeline/{institutionId}")
    public MedicalLifelineResponse analyzeMedicalLifeline(
            @PathVariable Long institutionId
    ) {
        return medicalLifelineService.analyze(institutionId);
    }
}