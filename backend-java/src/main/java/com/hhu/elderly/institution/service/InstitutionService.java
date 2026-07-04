package com.hhu.elderly.institution.service;

import com.hhu.elderly.institution.dto.InstitutionDetailResponse;
import com.hhu.elderly.institution.dto.InstitutionListItem;
import com.hhu.elderly.institution.repository.InstitutionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class InstitutionService {

    private final InstitutionRepository institutionRepository;

    public InstitutionService(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    public List<InstitutionListItem> listInstitutions(
            String keyword,
            String district,
            Integer institutionCategory,
            String gradeLevel,
            Integer priceTier,
            String serviceType,
            Boolean hasAvailableBeds,
            int limit,
            int offset
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 1000);
        int safeOffset = Math.max(offset, 0);

        return institutionRepository.findList(
                keyword,
                district,
                institutionCategory,
                gradeLevel,
                priceTier,
                serviceType,
                hasAvailableBeds,
                safeLimit,
                safeOffset
        );
    }

    public InstitutionDetailResponse getInstitutionDetail(Long id) {
        return institutionRepository.findDetailById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "未找到该养老机构"
                ));
    }
}