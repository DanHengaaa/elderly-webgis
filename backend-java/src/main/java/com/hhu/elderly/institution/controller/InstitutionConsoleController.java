package com.hhu.elderly.institution.controller;

import com.hhu.elderly.auth.security.AuthUserPrincipal;
import com.hhu.elderly.institution.dto.InstitutionApplicationRequest;
import com.hhu.elderly.institution.dto.InstitutionApplicationResponse;
import com.hhu.elderly.institution.dto.InstitutionConsoleInstitutionOption;
import com.hhu.elderly.institution.dto.InstitutionDetailResponse;
import com.hhu.elderly.institution.dto.InstitutionProfileUpdateRequest;
import com.hhu.elderly.institution.service.InstitutionApplicationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/institution-console")
public class InstitutionConsoleController {

    private final InstitutionApplicationService institutionApplicationService;

    public InstitutionConsoleController(
            InstitutionApplicationService institutionApplicationService
    ) {
        this.institutionApplicationService = institutionApplicationService;
    }

    @PostMapping("/applications")
    public Map<String, Object> createApplication(
            @RequestBody InstitutionApplicationRequest request,
            Authentication authentication
    ) {
        AuthUserPrincipal user = requireUser(authentication);
        return institutionApplicationService.createApplication(request, user);
    }

    @GetMapping("/applications/mine")
    public List<InstitutionApplicationResponse> mine(
            Authentication authentication
    ) {
        AuthUserPrincipal user = requireUser(authentication);
        return institutionApplicationService.findMine(user);
    }

    /**
     * 当前机构账号名下所有审核通过的机构。
     * 一个机构账号可以通过多次入驻申请绑定多家机构。
     */
    @GetMapping("/profile/options")
    public List<InstitutionConsoleInstitutionOption> myInstitutionOptions(
            Authentication authentication
    ) {
        AuthUserPrincipal user = requireUser(authentication);
        return institutionApplicationService.findMyInstitutionOptions(user);
    }

    /**
     * 兼容旧前端：返回当前账号最近维护的一家机构。
     */
    @GetMapping("/profile")
    public InstitutionDetailResponse myInstitution(
            Authentication authentication
    ) {
        AuthUserPrincipal user = requireUser(authentication);
        return institutionApplicationService.getMyInstitution(user);
    }

    /**
     * 获取当前账号绑定的指定机构详情。
     */
    @GetMapping("/profile/{institutionId}")
    public InstitutionDetailResponse myInstitutionById(
            @PathVariable Long institutionId,
            Authentication authentication
    ) {
        AuthUserPrincipal user = requireUser(authentication);
        return institutionApplicationService.getMyInstitution(user, institutionId);
    }

    /**
     * 兼容旧前端：修改当前账号最近维护的一家机构。
     */
    @PutMapping("/profile")
    public Map<String, Object> updateMyInstitution(
            @RequestBody InstitutionProfileUpdateRequest request,
            Authentication authentication
    ) {
        AuthUserPrincipal user = requireUser(authentication);
        return institutionApplicationService.updateMyInstitution(request, user);
    }

    /**
     * 修改当前账号绑定的指定机构详情。
     */
    @PutMapping("/profile/{institutionId}")
    public Map<String, Object> updateMyInstitutionById(
            @PathVariable Long institutionId,
            @RequestBody InstitutionProfileUpdateRequest request,
            Authentication authentication
    ) {
        AuthUserPrincipal user = requireUser(authentication);
        return institutionApplicationService.updateMyInstitution(institutionId, request, user);
    }

    private AuthUserPrincipal requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUserPrincipal user)) {
            throw new IllegalArgumentException("当前未登录。请先登录机构账号。");
        }

        return user;
    }
}
