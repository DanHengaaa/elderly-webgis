package com.hhu.elderly.institution.dto;

import java.time.LocalDateTime;

public record InstitutionApplicationResponse(
        Long id,
        Long userId,
        String institutionName,
        String contactPerson,
        String contactPhone,
        String district,
        String address,
        Double lon,
        Double lat,
        String licenseUrl,
        String recordCertificateUrl,
        String otherMaterialUrl,
        String description,
        String status,
        String statusText,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
}