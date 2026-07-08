package com.hhu.elderly.auth.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

public class AuthUserPrincipal {

    private final Long id;
    private final String username;
    private final String nickname;
    private final String roleCode;
    private final Long institutionId;

    public AuthUserPrincipal(
            Long id,
            String username,
            String nickname,
            String roleCode,
            Long institutionId
    ) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.roleCode = roleCode;
        this.institutionId = institutionId;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleCode));
    }
}