package com.hhu.elderly.institution.dto;

import java.time.LocalDateTime;

public record InstitutionConsoleInstitutionOption(
        Long id,
        String name,
        String district,
        String address,
        Integer status,
        String statusText,
        LocalDateTime updatedAt
) {
}
