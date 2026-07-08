package com.hhu.elderly.analysis.dto;

import java.util.List;

public record MedicalLifelineResponse(
        Long institutionId,
        String institutionName,
        String institutionAddress,
        String district,
        Double institutionLon,
        Double institutionLat,

        Double medicalAccessibilityIndex,
        String medicalAccessibilityLevel,
        String medicalAccessibilityLevelText,

        Double nearestMedicalDistanceKm,
        String nearestMedicalName,
        String nearestMedicalType,
        String nearestMedicalAddress,

        Double nearestKeyMedicalDistanceKm,
        String nearestKeyMedicalName,
        String nearestKeyMedicalType,
        String nearestKeyMedicalAddress,

        Double estimatedEmergencyMinutes,
        String emergencyResponseLevel,
        String emergencyResponseText,

        Integer medicalCount1km,
        Integer medicalCount3km,
        Integer medicalCount5km,
        Integer keyMedicalCount5km,
        Integer distinctMedicalTypeCount5km,

        List<RadiusSummary> radiusSummaries,
        List<NearbyMedicalPoi> nearbyMedicalPois
) {
    public record RadiusSummary(
            Integer radiusMeter,
            String radiusText,
            Integer medicalCount,
            Integer keyMedicalCount
    ) {
    }

    public record NearbyMedicalPoi(
            Long id,
            String name,
            String address,
            String district,
            String poiMid,
            String poiSmall,
            String typeFull,
            Double lon,
            Double lat,
            Double distanceKm,
            Boolean keyMedical
    ) {
    }
}