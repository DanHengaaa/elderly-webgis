package com.hhu.elderly.institution.service;

import com.hhu.elderly.auth.security.AuthUserPrincipal;
import com.hhu.elderly.institution.dto.InstitutionApplicationRequest;
import com.hhu.elderly.institution.dto.InstitutionApplicationResponse;
import com.hhu.elderly.institution.repository.InstitutionApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class InstitutionApplicationService {

    private final InstitutionApplicationRepository institutionApplicationRepository;

    public InstitutionApplicationService(
            InstitutionApplicationRepository institutionApplicationRepository
    ) {
        this.institutionApplicationRepository = institutionApplicationRepository;
    }

    public Map<String, Object> createApplication(
            InstitutionApplicationRequest request,
            AuthUserPrincipal user
    ) {
        Long id = institutionApplicationRepository.createApplication(
                user.getId(),
                request
        );

        return Map.of(
                "success", true,
                "id", id,
                "message", "入驻申请已提交，等待管理员审核。"
        );
    }

    public List<InstitutionApplicationResponse> findMine(AuthUserPrincipal user) {
        return institutionApplicationRepository.findMine(user.getId());
    }

    public List<InstitutionApplicationResponse> findByStatus(String status) {
        return institutionApplicationRepository.findByStatus(
                status == null || status.isBlank() ? "pending" : status
        );
    }

    public Map<String, Object> approve(
            Long id,
            AuthUserPrincipal operator
    ) {
        institutionApplicationRepository.updateStatus(
                id,
                "approved",
                "审核通过",
                operator.getId(),
                operator.getNickname()
        );

        return Map.of(
                "success", true,
                "message", "机构入驻申请已通过"
        );
    }

    public Map<String, Object> reject(
            Long id,
            String reason,
            AuthUserPrincipal operator
    ) {
        institutionApplicationRepository.updateStatus(
                id,
                "rejected",
                reason == null || reason.isBlank() ? "资质材料不完整或信息不符合要求" : reason,
                operator.getId(),
                operator.getNickname()
        );

        return Map.of(
                "success", true,
                "message", "机构入驻申请已驳回"
        );
    }
}