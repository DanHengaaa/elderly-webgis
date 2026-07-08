package com.hhu.elderly.analysis.service;

import com.hhu.elderly.analysis.dto.MedicalLifelineResponse;
import com.hhu.elderly.analysis.dto.MedicalLifelineResponse.NearbyMedicalPoi;
import com.hhu.elderly.analysis.dto.MedicalLifelineResponse.RadiusSummary;
import com.hhu.elderly.analysis.repository.MedicalLifelineRepository;
import com.hhu.elderly.analysis.repository.MedicalLifelineRepository.BasicInstitution;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MedicalLifelineService {

    private final MedicalLifelineRepository repository;

    public MedicalLifelineService(MedicalLifelineRepository repository) {
        this.repository = repository;
    }

    public MedicalLifelineResponse analyze(Long institutionId) {
        List<BasicInstitution> institutions = repository.findInstitutionById(institutionId);

        if (institutions.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "未找到机构，或该机构缺少空间坐标"
            );
        }

        BasicInstitution institution = institutions.get(0);

        int medicalCount1km = repository.countMedicalWithin(institutionId, 1000, false);
        int medicalCount3km = repository.countMedicalWithin(institutionId, 3000, false);
        int medicalCount5km = repository.countMedicalWithin(institutionId, 5000, false);

        int keyMedicalCount1km = repository.countMedicalWithin(institutionId, 1000, true);
        int keyMedicalCount3km = repository.countMedicalWithin(institutionId, 3000, true);
        int keyMedicalCount5km = repository.countMedicalWithin(institutionId, 5000, true);

        int distinctTypeCount5km = repository.countDistinctMedicalTypesWithin(institutionId, 5000);

        List<NearbyMedicalPoi> nearbyMedicalPois = repository.findNearbyMedicalPois(
                institutionId,
                50000,
                20
        );

        NearbyMedicalPoi nearestMedical = nearbyMedicalPois.isEmpty() ? null : nearbyMedicalPois.get(0);

        List<NearbyMedicalPoi> nearestKeyMedicalPois = repository.findNearestKeyMedicalPoi(institutionId);
        NearbyMedicalPoi nearestKeyMedical = nearestKeyMedicalPois.isEmpty() ? null : nearestKeyMedicalPois.get(0);

        Double nearestMedicalDistanceKm = nearestMedical == null ? null : nearestMedical.distanceKm();
        Double nearestKeyMedicalDistanceKm = nearestKeyMedical == null ? null : nearestKeyMedical.distanceKm();

        /*
         * 医疗资源可达性指数：
         * 1. distanceScore：根据最近重点医院距离赋分
         *    重点医院 = 三级甲等医院 / 综合医院
         *
         * 2. keyMedicalScore：根据 3km 范围内重点医院数量赋分
         *    keyMedicalScore = min(3km 内重点医院数量 / 3.0, 1.0)
         *    即 3km 内有 3 家及以上重点医院即满分
         *
         * 3. diversityScore：根据 5km 内医疗类型多样性赋分
         */
        Double accessibilityIndex = calculateAccessibilityIndex(
                nearestKeyMedicalDistanceKm,
                keyMedicalCount3km,
                distinctTypeCount5km
        );

        String accessibilityLevel = getAccessibilityLevel(accessibilityIndex);
        String accessibilityLevelText = getAccessibilityLevelText(accessibilityLevel);

        Double estimatedEmergencyMinutes = estimateEmergencyMinutes(nearestMedicalDistanceKm);
        String emergencyLevel = getEmergencyLevel(estimatedEmergencyMinutes);
        String emergencyText = getEmergencyText(emergencyLevel);

        List<RadiusSummary> radiusSummaries = List.of(
                new RadiusSummary(1000, "1 公里", medicalCount1km, keyMedicalCount1km),
                new RadiusSummary(3000, "3 公里", medicalCount3km, keyMedicalCount3km),
                new RadiusSummary(5000, "5 公里", medicalCount5km, keyMedicalCount5km)
        );

        return new MedicalLifelineResponse(
                institution.id(),
                institution.name(),
                institution.address(),
                institution.district(),
                institution.lon(),
                institution.lat(),

                accessibilityIndex,
                accessibilityLevel,
                accessibilityLevelText,

                nearestMedicalDistanceKm,
                nearestMedical == null ? null : nearestMedical.name(),
                nearestMedical == null ? null : getMedicalTypeText(nearestMedical),
                nearestMedical == null ? null : nearestMedical.address(),

                nearestKeyMedicalDistanceKm,
                nearestKeyMedical == null ? null : nearestKeyMedical.name(),
                nearestKeyMedical == null ? null : getMedicalTypeText(nearestKeyMedical),
                nearestKeyMedical == null ? null : nearestKeyMedical.address(),

                estimatedEmergencyMinutes,
                emergencyLevel,
                emergencyText,

                medicalCount1km,
                medicalCount3km,
                medicalCount5km,
                keyMedicalCount5km,
                distinctTypeCount5km,

                radiusSummaries,
                nearbyMedicalPois
        );
    }

    private Double calculateAccessibilityIndex(
            Double nearestKeyMedicalDistanceKm,
            int keyMedicalCount3km,
            int distinctTypeCount5km
    ) {
        double distanceScore = calculateKeyMedicalDistanceScore(nearestKeyMedicalDistanceKm);

        /*
         * 关键修改：
         * 原来使用 5km 内重点医院数量，条件偏宽松；
         * 现在改为 3km 内重点医院数量，3 家及以上满分。
         */
        double keyMedicalScore = Math.min(keyMedicalCount3km / 3.0, 1.0);

        double diversityScore = Math.min(distinctTypeCount5km / 5.0, 1.0);

        double result = 0.45 * distanceScore
                + 0.35 * keyMedicalScore
                + 0.20 * diversityScore;

        return round(result, 3);
    }

    private double calculateKeyMedicalDistanceScore(Double nearestKeyMedicalDistanceKm) {
        if (nearestKeyMedicalDistanceKm == null) {
            return 0.0;
        }

        if (nearestKeyMedicalDistanceKm <= 1.0) {
            return 1.0;
        }

        if (nearestKeyMedicalDistanceKm <= 3.0) {
            return 0.8;
        }

        if (nearestKeyMedicalDistanceKm <= 5.0) {
            return 0.6;
        }

        if (nearestKeyMedicalDistanceKm <= 8.0) {
            return 0.4;
        }

        return 0.2;
    }

    private Double estimateEmergencyMinutes(Double nearestMedicalDistanceKm) {
        if (nearestMedicalDistanceKm == null) {
            return null;
        }

        // 简化估算：4 分钟调度准备时间 + 按 30km/h 城市急救车平均速度估算行驶时间
        double minutes = 4.0 + nearestMedicalDistanceKm / 30.0 * 60.0;
        return round(minutes, 1);
    }

    private String getAccessibilityLevel(Double index) {
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

    private String getAccessibilityLevelText(String level) {
        return switch (level) {
            case "excellent" -> "医疗资源可达性优";
            case "good" -> "医疗资源可达性良好";
            case "medium" -> "医疗资源可达性一般";
            case "weak" -> "医疗资源可达性较弱";
            default -> "暂无可达性评价";
        };
    }

    private String getEmergencyLevel(Double minutes) {
        if (minutes == null) {
            return "unknown";
        }

        if (minutes <= 15) {
            return "high";
        }

        if (minutes <= 30) {
            return "medium";
        }

        return "low";
    }

    private String getEmergencyText(String level) {
        return switch (level) {
            case "high" -> "高保障";
            case "medium" -> "中保障";
            case "low" -> "低保障";
            default -> "暂无评估";
        };
    }

    private String getMedicalTypeText(NearbyMedicalPoi poi) {
        if (poi.poiSmall() != null && !poi.poiSmall().isBlank()) {
            return poi.poiSmall();
        }

        if (poi.typeFull() != null && !poi.typeFull().isBlank()) {
            return poi.typeFull();
        }

        if (poi.poiMid() != null && !poi.poiMid().isBlank()) {
            return poi.poiMid();
        }

        return "医疗设施";
    }

    private Double round(Double value, int scale) {
        if (value == null) {
            return null;
        }

        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}