package com.hhu.elderly.admin.controller;

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
            @RequestParam(required = false, defaultValue = "pending") String status,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return institutionApplicationService.findByStatus(status);
    }

    @PostMapping("/{id}/approve")
    public Map<String, Object> approve(
            @PathVariable Long id,
            Authentication authentication
    ) {
        AuthUserPrincipal admin = requireAdmin(authentication);
        return institutionApplicationService.approve(id, admin);
    }

    @PostMapping("/{id}/reject")
    public Map<String, Object> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication
    ) {
        AuthUserPrincipal admin = requireAdmin(authentication);
        String reason = body == null ? null : body.get("reason");
        return institutionApplicationService.reject(id, reason, admin);
    }

    private AuthUserPrincipal requireAdmin(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal user)) {
            throw new IllegalArgumentException("当前未登录。请先登录管理员账号。");
        }

        if (!"ADMIN".equals(user.getRoleCode())) {
            throw new IllegalArgumentException("当前账号不是管理员，不能审核机构入驻申请。");
        }

        return user;
    }
}
