package com.hhu.elderly.institution.controller;

import com.hhu.elderly.institution.dto.InstitutionDetailResponse;
import com.hhu.elderly.institution.dto.InstitutionListItem;
import com.hhu.elderly.institution.service.InstitutionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/institutions")
public class InstitutionController {

    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @GetMapping
    public List<InstitutionListItem> listInstitutions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Integer institutionCategory,
            @RequestParam(required = false) String gradeLevel,
            @RequestParam(required = false) Integer priceTier,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) Boolean hasAvailableBeds,
            @RequestParam(defaultValue = "500") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return institutionService.listInstitutions(
                keyword,
                district,
                institutionCategory,
                gradeLevel,
                priceTier,
                serviceType,
                hasAvailableBeds,
                limit,
                offset
        );
    }

    @GetMapping("/{id}")
    public InstitutionDetailResponse getInstitutionDetail(@PathVariable Long id) {
        return institutionService.getInstitutionDetail(id);
    }
}