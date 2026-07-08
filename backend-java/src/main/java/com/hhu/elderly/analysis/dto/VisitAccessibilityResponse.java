package com.hhu.elderly.analysis.dto;

import java.util.List;

public record VisitAccessibilityResponse(
        Double startLon,
        Double startLat,
        String startName,
        String mode,

        List<IsochroneRing> rings,
        List<ReachableInstitution> reachableInstitutions
) {
    public record IsochroneRing(
            Integer minutes,
            Double seconds,
            String level,
            String levelText,
            String polygonGeoJson
    ) {
    }

    public record ReachableInstitution(
            Long id,
            String name,
            String address,
            String district,
            Double lon,
            Double lat,
            Double estimatedMinutes,
            String reachabilityLevel,
            String reachabilityLevelText
    ) {
    }
}