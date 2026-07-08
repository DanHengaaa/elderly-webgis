package com.hhu.elderly.analysis.dto;

public record GeoCodeResponse(
        String keyword,
        String formattedAddress,
        Double lon,
        Double lat
) {
}