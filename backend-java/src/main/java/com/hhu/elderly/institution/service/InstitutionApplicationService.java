package com.hhu.elderly.institution.service;

import com.hhu.elderly.auth.security.AuthUserPrincipal;
import com.hhu.elderly.institution.dto.InstitutionApplicationRequest;
import com.hhu.elderly.institution.dto.InstitutionApplicationResponse;
import com.hhu.elderly.institution.dto.InstitutionConsoleInstitutionOption;
import com.hhu.elderly.institution.dto.InstitutionDetailResponse;
import com.hhu.elderly.institution.dto.InstitutionProfileUpdateRequest;
import com.hhu.elderly.institution.repository.InstitutionApplicationRepository;
import com.hhu.elderly.institution.repository.InstitutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class InstitutionApplicationService {

    private final InstitutionApplicationRepository institutionApplicationRepository;
    private final InstitutionRepository institutionRepository;

    public InstitutionApplicationService(
            InstitutionApplicationRepository institutionApplicationRepository,
            InstitutionRepository institutionRepository
    ) {
        this.institutionApplicationRepository = institutionApplicationRepository;
        this.institutionRepository = institutionRepository;
    }

    public Map<String, Object> createApplication(
            InstitutionApplicationRequest request,
            AuthUserPrincipal user
    ) {
        validateApplication(request);

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

    public List<InstitutionConsoleInstitutionOption> findMyInstitutionOptions(AuthUserPrincipal user) {
        return institutionApplicationRepository.findOwnedInstitutionOptions(user.getId());
    }

    public InstitutionDetailResponse getMyInstitution(AuthUserPrincipal user) {
        Long institutionId = institutionApplicationRepository.findOwnedInstitutionId(user.getId());

        if (institutionId == null) {
            return null;
        }

        return institutionRepository.findDetailById(institutionId).orElse(null);
    }

    public InstitutionDetailResponse getMyInstitution(AuthUserPrincipal user, Long institutionId) {
        if (institutionId == null) {
            return getMyInstitution(user);
        }

        if (!institutionApplicationRepository.isOwnedInstitution(user.getId(), institutionId)) {
            throw new IllegalArgumentException("只能查看当前机构账号名下的机构。");
        }

        return institutionRepository.findDetailById(institutionId).orElse(null);
    }

    @Transactional
    public Map<String, Object> updateMyInstitution(
            InstitutionProfileUpdateRequest request,
            AuthUserPrincipal user
    ) {
        Long institutionId = institutionApplicationRepository.findOwnedInstitutionId(user.getId());
        return updateMyInstitution(institutionId, request, user);
    }

    @Transactional
    public Map<String, Object> updateMyInstitution(
            Long institutionId,
            InstitutionProfileUpdateRequest request,
            AuthUserPrincipal user
    ) {
        validateInstitutionProfile(request);

        if (institutionId == null) {
            throw new IllegalArgumentException("当前账号还没有审核通过的绑定机构，暂不能修改机构详情。");
        }

        institutionApplicationRepository.updateOwnedInstitution(
                user.getId(),
                institutionId,
                request
        );

        institutionApplicationRepository.refreshSpatialIndex(institutionId);

        return Map.of(
                "success", true,
                "id", institutionId,
                "message", "机构信息已保存，主页、详情页和推荐候选库会同步显示最新内容。"
        );
    }

    @Transactional
    public Map<String, Object> approve(
            Long id,
            AuthUserPrincipal operator
    ) {
        Long institutionId = institutionApplicationRepository.approveAndPublishApplication(
                id,
                operator.getId(),
                displayName(operator)
        );

        return Map.of(
                "success", true,
                "institutionId", institutionId,
                "message", "机构入驻申请已通过，机构已发布到地图与推荐候选库。"
        );
    }

    @Transactional
    public Map<String, Object> reject(
            Long id,
            String reason,
            AuthUserPrincipal operator
    ) {
        institutionApplicationRepository.rejectApplication(
                id,
                reason == null || reason.isBlank() ? "资质材料不完整或信息不符合要求" : reason,
                operator.getId(),
                displayName(operator)
        );

        return Map.of(
                "success", true,
                "message", "机构入驻申请已驳回"
        );
    }

    private void validateApplication(InstitutionApplicationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("申请信息不能为空。");
        }

        if (!hasText(request.institutionName())) {
            throw new IllegalArgumentException("请填写机构名称。");
        }

        if (!hasText(request.province()) || !hasText(request.city()) || !hasText(request.district())) {
            throw new IllegalArgumentException("请选择省、市、区县。");
        }

        if (!hasText(request.address())) {
            throw new IllegalArgumentException("请通过天地图匹配或地图点选设置机构地址。");
        }

        if (request.lon() == null || request.lat() == null) {
            throw new IllegalArgumentException("请通过天地图匹配或地图点选设置机构点位，不要手动填写经纬度。");
        }

        if (!hasText(request.contactPhone())) {
            throw new IllegalArgumentException("请填写联系电话。");
        }
    }

    private void validateInstitutionProfile(InstitutionProfileUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("机构信息不能为空。");
        }

        if (!hasText(request.name())) {
            throw new IllegalArgumentException("请填写机构名称。");
        }

        if (!hasText(request.province()) || !hasText(request.city()) || !hasText(request.district())) {
            throw new IllegalArgumentException("请选择省、市、区县。");
        }

        if (!hasText(request.address())) {
            throw new IllegalArgumentException("请设置机构地址。");
        }

        if (request.lon() == null || request.lat() == null) {
            throw new IllegalArgumentException("请通过天地图匹配或地图点选设置机构点位。");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String displayName(AuthUserPrincipal user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }

        return user.getUsername();
    }
}
