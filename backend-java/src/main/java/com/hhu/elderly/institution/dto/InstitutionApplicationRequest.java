package com.hhu.elderly.institution.dto;

public record InstitutionApplicationRequest(
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
        String description
) {
}