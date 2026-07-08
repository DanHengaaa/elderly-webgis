package com.hhu.elderly.admin.controller;

import com.hhu.elderly.admin.dto.AdminReviewRequest;
import com.hhu.elderly.auth.security.AuthUserPrincipal;
import com.hhu.elderly.institution.dto.InstitutionApplicationResponse;
import com.hhu.elderly.institution.service.InstitutionApplicationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/institution-applications")
public class AdminInstitutionApplicationController {

    private final InstitutionApplicationService institutionApplicationService;

    public AdminInstitutionApplicationController(
            InstitutionApplicationService institutionApplicationService
    ) {
        this.institutionApplicationService = institutionApplicationService;
    }

    @GetMapping
    public List<InstitutionApplicationResponse> list(
            @RequestParam(required = false, defaultValue = "pending") String status
    ) {
        return institutionApplicationService.findByStatus(status);
    }

    @PostMapping("/{id}/approve")
    public Map<String, Object> approve(
            @PathVariable Long id,
            Authentication authentication
    ) {
        AuthUserPrincipal user = (AuthUserPrincipal) authentication.getPrincipal();
        return institutionApplicationService.approve(id, user);
    }

    @PostMapping("/{id}/reject")
    public Map<String, Object> reject(
            @PathVariable Long id,
            @RequestBody(required = false) AdminReviewRequest request,
            Authentication authentication
    ) {
        AuthUserPrincipal user = (AuthUserPrincipal) authentication.getPrincipal();
        String reason = request == null ? null : request.reason();

        return institutionApplicationService.reject(id, reason, user);
    }
}