package com.hhu.elderly.analysis.dto;

import java.util.List;

public record EnvironmentAnalysisResponse(
        Long institutionId,
        String institutionName,
        String institutionAddress,
        String district,
        Double institutionLon,
        Double institutionLat,

        Double lifeConvenienceIndex,
        String lifeConvenienceLevel,
        String lifeConvenienceLevelText,

        Double greenLeisureIndex,
        String greenLeisureLevel,
        String greenLeisureLevelText,

        Double environmentSuitabilityIndex,
        String environmentSuitabilityLevel,
        String environmentSuitabilityLevelText,

        Integer lifeServiceCount600m,
        Integer lifeServiceCount1km,
        Integer lifeServiceCount3km,
        Integer lifeServiceTypeCount600m,

        Integer parkCount600m,
        Integer parkCount1km,
        Integer parkCount3km,
        Integer parkTypeCount3km,

        Double nearestLifeServiceDistanceKm,
        String nearestLifeServiceName,
        String nearestLifeServiceType,
        String nearestLifeServiceAddress,

        Double nearestParkDistanceKm,
        String nearestParkName,
        String nearestParkType,
        String nearestParkAddress,

        List<EnvironmentRadiusSummary> radiusSummaries,
        List<EnvironmentPoi> lifeServicePois,
        List<EnvironmentPoi> parkPois
) {
    public record EnvironmentRadiusSummary(
            Integer radiusMeter,
            String radiusText,
            Integer lifeServiceCount,
            Integer parkCount
    ) {
    }

    public record EnvironmentPoi(
            Long id,
            Integer category,
            String categoryText,
            String name,
            String address,
            String district,
            String poiMid,
            String poiSmall,
            String typeFull,
            Double lon,
            Double lat,
            Double distanceKm
    ) {
    }
}