package com.hhu.elderly.institution.controller;

import com.hhu.elderly.auth.security.AuthUserPrincipal;
import com.hhu.elderly.institution.dto.InstitutionApplicationRequest;
import com.hhu.elderly.institution.dto.InstitutionApplicationResponse;
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
        AuthUserPrincipal user = (AuthUserPrincipal) authentication.getPrincipal();
        return institutionApplicationService.createApplication(request, user);
    }

    @GetMapping("/applications/mine")
    public List<InstitutionApplicationResponse> mine(
            Authentication authentication
    ) {
        AuthUserPrincipal user = (AuthUserPrincipal) authentication.getPrincipal();
        return institutionApplicationService.findMine(user);
    }
}