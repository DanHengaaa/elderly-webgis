package com.hhu.elderly.analysis.service;

import com.hhu.elderly.analysis.dto.EnvironmentAnalysisResponse;
import com.hhu.elderly.analysis.dto.EnvironmentAnalysisResponse.EnvironmentPoi;
import com.hhu.elderly.analysis.dto.EnvironmentAnalysisResponse.EnvironmentRadiusSummary;
import com.hhu.elderly.analysis.repository.EnvironmentAnalysisRepository;
import com.hhu.elderly.analysis.repository.EnvironmentAnalysisRepository.BasicInstitution;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EnvironmentAnalysisService {

    private static final int LIFE_CATEGORY = 2;
    private static final int PARK_CATEGORY = 3;

    private final EnvironmentAnalysisRepository repository;

    public EnvironmentAnalysisService(EnvironmentAnalysisRepository repository) {
        this.repository = repository;
    }

    public EnvironmentAnalysisResponse analyze(Long institutionId) {
        List<BasicInstitution> institutions = repository.findInstitutionById(institutionId);

        if (institutions.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "未找到机构，或该机构缺少空间坐标"
            );
        }

        BasicInstitution institution = institutions.get(0);

        int lifeCount600m = repository.countPoiWithin(institutionId, LIFE_CATEGORY, 600);
        int lifeCount1km = repository.countPoiWithin(institutionId, LIFE_CATEGORY, 1000);
        int lifeCount3km = repository.countPoiWithin(institutionId, LIFE_CATEGORY, 3000);
        int lifeTypeCount600m = repository.countDistinctPoiTypesWithin(institutionId, LIFE_CATEGORY, 600);

        int parkCount600m = repository.countPoiWithin(institutionId, PARK_CATEGORY, 600);
        int parkCount1km = repository.countPoiWithin(institutionId, PARK_CATEGORY, 1000);
        int parkCount3km = repository.countPoiWithin(institutionId, PARK_CATEGORY, 3000);
        int parkTypeCount3km = repository.countDistinctPoiTypesWithin(institutionId, PARK_CATEGORY, 3000);

        List<EnvironmentPoi> nearestLifePois = repository.findNearestPoi(institutionId, LIFE_CATEGORY);
        List<EnvironmentPoi> nearestParkPois = repository.findNearestPoi(institutionId, PARK_CATEGORY);

        EnvironmentPoi nearestLifePoi = nearestLifePois.isEmpty() ? null : nearestLifePois.get(0);
        EnvironmentPoi nearestParkPoi = nearestParkPois.isEmpty() ? null : nearestParkPois.get(0);

        Double nearestLifeDistanceKm = nearestLifePoi == null ? null : nearestLifePoi.distanceKm();
        Double nearestParkDistanceKm = nearestParkPoi == null ? null : nearestParkPoi.distanceKm();

        List<EnvironmentPoi> lifeServicePois = repository.findNearbyPois(
                institutionId,
                LIFE_CATEGORY,
                3000,
                40
        );

        List<EnvironmentPoi> parkPois = repository.findNearbyPois(
                institutionId,
                PARK_CATEGORY,
                5000,
                40
        );

        Double lifeConvenienceIndex = calculateLifeConvenienceIndex(
                lifeCount600m,
                lifeTypeCount600m,
                nearestLifeDistanceKm
        );

        Double greenLeisureIndex = calculateGreenLeisureIndex(
                nearestParkDistanceKm,
                parkCount3km,
                parkTypeCount3km
        );

        Double environmentSuitabilityIndex = round(
                0.60 * lifeConvenienceIndex + 0.40 * greenLeisureIndex,
                3
        );

        String lifeLevel = getLevel(lifeConvenienceIndex);
        String greenLevel = getLevel(greenLeisureIndex);
        String environmentLevel = getLevel(environmentSuitabilityIndex);

        List<EnvironmentRadiusSummary> radiusSummaries = List.of(
                new EnvironmentRadiusSummary(600, "600 米生活圈", lifeCount600m, parkCount600m),
                new EnvironmentRadiusSummary(1000, "1 公里", lifeCount1km, parkCount1km),
                new EnvironmentRadiusSummary(3000, "3 公里", lifeCount3km, parkCount3km)
        );

        return new EnvironmentAnalysisResponse(
                institution.id(),
                institution.name(),
                institution.address(),
                institution.district(),
                institution.lon(),
                institution.lat(),

                lifeConvenienceIndex,
                lifeLevel,
                getLevelText(lifeLevel, "生活便利度"),

                greenLeisureIndex,
                greenLevel,
                getLevelText(greenLevel, "绿色休闲"),

                environmentSuitabilityIndex,
                environmentLevel,
                getLevelText(environmentLevel, "综合环境宜居"),

                lifeCount600m,
                lifeCount1km,
                lifeCount3km,
                lifeTypeCount600m,

                parkCount600m,
                parkCount1km,
                parkCount3km,
                parkTypeCount3km,

                nearestLifeDistanceKm,
                nearestLifePoi == null ? null : nearestLifePoi.name(),
                nearestLifePoi == null ? null : getPoiTypeText(nearestLifePoi),
                nearestLifePoi == null ? null : nearestLifePoi.address(),

                nearestParkDistanceKm,
                nearestParkPoi == null ? null : nearestParkPoi.name(),
                nearestParkPoi == null ? null : getPoiTypeText(nearestParkPoi),
                nearestParkPoi == null ? null : nearestParkPoi.address(),

                radiusSummaries,
                lifeServicePois,
                parkPois
        );
    }

    private Double calculateLifeConvenienceIndex(
            int lifeCount600m,
            int lifeTypeCount600m,
            Double nearestLifeDistanceKm
    ) {
        /*
         * 生活便利度指数：
         * 1. 600m 生活圈设施密度，20 个及以上满分
         * 2. 600m 生活圈设施类型多样性，5 类及以上满分
         * 3. 最近生活服务设施距离
         */
        double densityScore = Math.min(lifeCount600m / 20.0, 1.0);
        double diversityScore = Math.min(lifeTypeCount600m / 5.0, 1.0);
        double distanceScore = calculateLifeDistanceScore(nearestLifeDistanceKm);

        double result = 0.45 * densityScore
                + 0.35 * diversityScore
                + 0.20 * distanceScore;

        return round(result, 3);
    }

    private Double calculateGreenLeisureIndex(
            Double nearestParkDistanceKm,
            int parkCount3km,
            int parkTypeCount3km
    ) {
        /*
         * 绿色休闲指数：
         * 1. 最近公园景点距离
         * 2. 3km 范围内公园景点数量，5 个及以上满分
         * 3. 3km 范围内公园景点类型多样性，3 类及以上满分
         */
        double distanceScore = calculateParkDistanceScore(nearestParkDistanceKm);
        double countScore = Math.min(parkCount3km / 5.0, 1.0);
        double diversityScore = Math.min(parkTypeCount3km / 3.0, 1.0);

        double result = 0.60 * distanceScore
                + 0.25 * countScore
                + 0.15 * diversityScore;

        return round(result, 3);
    }

    private double calculateLifeDistanceScore(Double distanceKm) {
        if (distanceKm == null) {
            return 0.0;
        }

        if (distanceKm <= 0.3) {
            return 1.0;
        }

        if (distanceKm <= 0.6) {
            return 0.85;
        }

        if (distanceKm <= 1.0) {
            return 0.65;
        }

        if (distanceKm <= 1.5) {
            return 0.45;
        }

        return 0.25;
    }

    private double calculateParkDistanceScore(Double distanceKm) {
        if (distanceKm == null) {
            return 0.0;
        }

        if (distanceKm <= 0.3) {
            return 1.0;
        }

        if (distanceKm <= 0.6) {
            return 0.85;
        }

        if (distanceKm <= 1.0) {
            return 0.70;
        }

        if (distanceKm <= 2.0) {
            return 0.50;
        }

        if (distanceKm <= 3.0) {
            return 0.35;
        }

        return 0.15;
    }

    private String getLevel(Double index) {
        if (index == null) {
            return "unknown";
        }

        if (index >= 0.75) {
            return "excellent";
        }

        if (index >= 0.55) {
            return "good";
        }

        if (index >= 0.35) {
            return "medium";
        }

        return "weak";
    }

    private String getLevelText(String level, String prefix) {
        return switch (level) {
            case "excellent" -> prefix + "优";
            case "good" -> prefix + "良好";
            case "medium" -> prefix + "一般";
            case "weak" -> prefix + "较弱";
            default -> "暂无评价";
        };
    }

    private String getPoiTypeText(EnvironmentPoi poi) {
        if (poi.poiSmall() != null && !poi.poiSmall().isBlank()) {
            return poi.poiSmall();
        }

        if (poi.typeFull() != null && !poi.typeFull().isBlank()) {
            return poi.typeFull();
        }

        if (poi.poiMid() != null && !poi.poiMid().isBlank()) {
            return poi.poiMid();
        }

        return poi.categoryText();
    }

    private Double round(Double value, int scale) {
        if (value == null) {
            return null;
        }

        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}