package com.hhu.elderly.recommendation.service;

import com.hhu.elderly.recommendation.dto.RecommendationRequest;
import com.hhu.elderly.recommendation.dto.RecommendationResponse;
import com.hhu.elderly.recommendation.dto.RecommendationResponse.RecommendationItem;
import com.hhu.elderly.recommendation.repository.RecommendationRepository;
import com.hhu.elderly.recommendation.repository.RecommendationRepository.CandidateInstitution;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {

    private final RecommendationRepository repository;

    public RecommendationService(RecommendationRepository repository) {
        this.repository = repository;
    }

    public RecommendationResponse recommend(RecommendationRequest request) {
        int limit = request.limit() == null ? 30 : Math.max(5, Math.min(request.limit(), 100));

        List<CandidateInstitution> candidates = repository.findCandidates(request, 200);

        Map<Long, Double> visitMinutesMap = Map.of();

        if (request.startLon() != null && request.startLat() != null) {
            visitMinutesMap = repository.findVisitMinutesByStart(
                    request.startLon(),
                    request.startLat(),
                    60 * 60
            );
        }

        WeightConfig weightConfig = buildWeightConfig(request);

        List<RecommendationItem> items = new ArrayList<>();

        for (CandidateInstitution candidate : candidates) {
            Double budgetScore = calculateBudgetScore(
                    candidate.monthlyFeeBase(),
                    request.budgetMin(),
                    request.budgetMax()
            );

            Double bedScore = calculateBedScore(candidate.availableBeds());

            Double ratingScore = calculateRatingScore(candidate.ratingAvg());

            Double medicalScore = calculateMedicalScore(
                    candidate.nearestKeyMedicalKm(),
                    safeInt(candidate.keyMedicalCount3km()),
                    safeInt(candidate.medicalTypeCount5km())
            );

            Double lifeScore = calculateLifeScore(
                    candidate.nearestLifeKm(),
                    safeInt(candidate.lifeCount600m()),
                    safeInt(candidate.lifeTypeCount600m())
            );

            Double greenScore = calculateGreenScore(
                    candidate.nearestParkKm(),
                    safeInt(candidate.parkCount3km()),
                    safeInt(candidate.parkTypeCount3km())
            );

            Double estimatedVisitMinutes = visitMinutesMap.get(candidate.id());
            Double visitScore = calculateVisitScore(estimatedVisitMinutes, request.startLon(), request.startLat());

            Double careScore = calculateCareScore(request.careLevel(), candidate);

            Double finalScore = round(
                    0.18 * careScore
                            + 0.18 * budgetScore
                            + 0.10 * bedScore
                            + 0.08 * ratingScore
                            + weightConfig.medicalWeight() * medicalScore
                            + weightConfig.lifeWeight() * lifeScore
                            + weightConfig.greenWeight() * greenScore
                            + weightConfig.visitWeight() * visitScore,
                    3
            );

            List<String> reasons = buildReasons(
                    candidate,
                    budgetScore,
                    bedScore,
                    medicalScore,
                    lifeScore,
                    greenScore,
                    visitScore,
                    estimatedVisitMinutes
            );

            RecommendationItem item = new RecommendationItem(
                    candidate.id(),
                    candidate.name(),
                    candidate.address(),
                    candidate.district(),
                    candidate.institutionCategory(),
                    getInstitutionCategoryText(candidate.institutionCategory()),
                    candidate.gradeLevel(),
                    candidate.totalBeds(),
                    candidate.availableBeds(),
                    candidate.monthlyFeeBase(),
                    candidate.priceTier(),
                    candidate.ratingAvg(),
                    candidate.ratingCount(),
                    candidate.coverImageUrl(),
                    candidate.lon(),
                    candidate.lat(),
                    finalScore,
                    getMatchLevel(finalScore),
                    getMatchLevelText(finalScore),
                    budgetScore,
                    bedScore,
                    ratingScore,
                    medicalScore,
                    lifeScore,
                    greenScore,
                    visitScore,
                    estimatedVisitMinutes,
                    reasons
            );

            items.add(item);
        }

        items = items.stream()
                .sorted(Comparator.comparing(RecommendationItem::finalScore).reversed())
                .limit(limit)
                .toList();

        return new RecommendationResponse(
                "已根据老人需求、预算、空间环境和探视可达性生成推荐结果",
                items.size(),
                items
        );
    }

    private WeightConfig buildWeightConfig(RecommendationRequest request) {
        double medicalPriority = normalizePriority(request.medicalPriority(), 3.0);
        double lifePriority = normalizePriority(request.lifePriority(), 3.0);
        double greenPriority = normalizePriority(request.greenPriority(), 3.0);
        double visitPriority = normalizePriority(request.visitPriority(), 3.0);

        double total = medicalPriority + lifePriority + greenPriority + visitPriority;

        if (total <= 0) {
            total = 12.0;
            medicalPriority = 3.0;
            lifePriority = 3.0;
            greenPriority = 3.0;
            visitPriority = 3.0;
        }

        /*
         * 空间类指标总权重为 0.46。
         * 其内部按照用户偏好动态分配。
         */
        double spatialWeight = 0.46;

        return new WeightConfig(
                spatialWeight * medicalPriority / total,
                spatialWeight * lifePriority / total,
                spatialWeight * greenPriority / total,
                spatialWeight * visitPriority / total
        );
    }

    private double normalizePriority(Double value, Double defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        if (value < 1) {
            return 1;
        }

        if (value > 5) {
            return 5;
        }

        return value;
    }

    private Double calculateBudgetScore(
            BigDecimal monthlyFee,
            BigDecimal budgetMin,
            BigDecimal budgetMax
    ) {
        if (monthlyFee == null) {
            return 0.55;
        }

        double fee = monthlyFee.doubleValue();

        double min = budgetMin == null ? 0 : budgetMin.doubleValue();
        double max = budgetMax == null ? Double.MAX_VALUE : budgetMax.doubleValue();

        if (fee >= min && fee <= max) {
            return 1.0;
        }

        if (fee < min) {
            return 0.85;
        }

        if (max == Double.MAX_VALUE || max <= 0) {
            return 0.7;
        }

        double overRatio = (fee - max) / max;

        if (overRatio <= 0.1) {
            return 0.75;
        }

        if (overRatio <= 0.2) {
            return 0.55;
        }

        if (overRatio <= 0.3) {
            return 0.35;
        }

        return 0.15;
    }

    private Double calculateBedScore(Integer availableBeds) {
        if (availableBeds == null) {
            return 0.5;
        }

        if (availableBeds <= 0) {
            return 0.1;
        }

        return round(Math.min(availableBeds / 20.0, 1.0), 3);
    }

    private Double calculateRatingScore(BigDecimal ratingAvg) {
        if (ratingAvg == null) {
            return 0.6;
        }

        return round(Math.min(ratingAvg.doubleValue() / 5.0, 1.0), 3);
    }

    private Double calculateMedicalScore(
            Double nearestKeyMedicalKm,
            int keyMedicalCount3km,
            int medicalTypeCount5km
    ) {
        double distanceScore = calculateKeyMedicalDistanceScore(nearestKeyMedicalKm);
        double keyCountScore = Math.min(keyMedicalCount3km / 3.0, 1.0);
        double diversityScore = Math.min(medicalTypeCount5km / 5.0, 1.0);

        return round(
                0.45 * distanceScore
                        + 0.35 * keyCountScore
                        + 0.20 * diversityScore,
                3
        );
    }

    private double calculateKeyMedicalDistanceScore(Double nearestKeyMedicalKm) {
        if (nearestKeyMedicalKm == null) {
            return 0.0;
        }

        if (nearestKeyMedicalKm <= 1.0) {
            return 1.0;
        }

        if (nearestKeyMedicalKm <= 3.0) {
            return 0.8;
        }

        if (nearestKeyMedicalKm <= 5.0) {
            return 0.6;
        }

        if (nearestKeyMedicalKm <= 8.0) {
            return 0.4;
        }

        return 0.2;
    }

    private Double calculateLifeScore(
            Double nearestLifeKm,
            int lifeCount600m,
            int lifeTypeCount600m
    ) {
        double densityScore = Math.min(lifeCount600m / 20.0, 1.0);
        double diversityScore = Math.min(lifeTypeCount600m / 5.0, 1.0);
        double distanceScore = calculateLifeDistanceScore(nearestLifeKm);

        return round(
                0.45 * densityScore
                        + 0.35 * diversityScore
                        + 0.20 * distanceScore,
                3
        );
    }

    private double calculateLifeDistanceScore(Double nearestLifeKm) {
        if (nearestLifeKm == null) {
            return 0.0;
        }

        if (nearestLifeKm <= 0.3) {
            return 1.0;
        }

        if (nearestLifeKm <= 0.6) {
            return 0.8;
        }

        if (nearestLifeKm <= 1.0) {
            return 0.6;
        }

        if (nearestLifeKm <= 2.0) {
            return 0.4;
        }

        return 0.2;
    }

    private Double calculateGreenScore(
            Double nearestParkKm,
            int parkCount3km,
            int parkTypeCount3km
    ) {
        double distanceScore = calculateParkDistanceScore(nearestParkKm);
        double countScore = Math.min(parkCount3km / 5.0, 1.0);
        double diversityScore = Math.min(parkTypeCount3km / 3.0, 1.0);

        return round(
                0.60 * distanceScore
                        + 0.25 * countScore
                        + 0.15 * diversityScore,
                3
        );
    }

    private double calculateParkDistanceScore(Double nearestParkKm) {
        if (nearestParkKm == null) {
            return 0.0;
        }

        if (nearestParkKm <= 0.5) {
            return 1.0;
        }

        if (nearestParkKm <= 1.0) {
            return 0.8;
        }

        if (nearestParkKm <= 2.0) {
            return 0.6;
        }

        if (nearestParkKm <= 3.0) {
            return 0.4;
        }

        return 0.2;
    }

    private Double calculateVisitScore(
            Double estimatedVisitMinutes,
            Double startLon,
            Double startLat
    ) {
        if (startLon == null || startLat == null) {
            return 0.6;
        }

        if (estimatedVisitMinutes == null) {
            return 0.15;
        }

        if (estimatedVisitMinutes <= 15) {
            return 1.0;
        }

        if (estimatedVisitMinutes <= 30) {
            return 0.8;
        }

        if (estimatedVisitMinutes <= 60) {
            return 0.55;
        }

        return 0.2;
    }

    private Double calculateCareScore(
            String careLevel,
            CandidateInstitution candidate
    ) {
        if (careLevel == null || careLevel.isBlank()) {
            return 0.7;
        }

        String level = careLevel.trim();

        if ("selfCare".equals(level)) {
            return 0.85;
        }

        if ("semiCare".equals(level)) {
            if (safeInt(candidate.totalBeds()) >= 100) {
                return 0.85;
            }
            return 0.7;
        }

        if ("nursing".equals(level)) {
            if (containsAny(candidate.gradeLevel(), "护理", "医养", "护理院")) {
                return 0.9;
            }
            return 0.65;
        }

        if ("dementia".equals(level)) {
            if (containsAny(candidate.gradeLevel(), "护理", "医养", "专护", "照护")) {
                return 0.85;
            }
            return 0.6;
        }

        return 0.7;
    }

    private List<String> buildReasons(
            CandidateInstitution candidate,
            Double budgetScore,
            Double bedScore,
            Double medicalScore,
            Double lifeScore,
            Double greenScore,
            Double visitScore,
            Double estimatedVisitMinutes
    ) {
        List<String> reasons = new ArrayList<>();

        if (budgetScore >= 0.9) {
            reasons.add("费用水平与预算匹配度较高");
        }

        if (bedScore >= 0.7) {
            reasons.add("当前可用床位较充足");
        }

        if (medicalScore >= 0.75) {
            reasons.add("周边重点医疗资源较好");
        }

        if (lifeScore >= 0.75) {
            reasons.add("周边生活服务设施较完善");
        }

        if (greenScore >= 0.75) {
            reasons.add("周边公园景点资源较丰富");
        }

        if (estimatedVisitMinutes != null) {
            reasons.add("从探视起点出发约 " + round(estimatedVisitMinutes, 1) + " 分钟可达");
        } else if (visitScore < 0.3) {
            reasons.add("探视起点 60 分钟路网范围内暂未匹配到该机构");
        }

        if (reasons.isEmpty()) {
            reasons.add("综合条件较均衡，可作为备选机构进一步查看");
        }

        return reasons;
    }

    private String getInstitutionCategoryText(Integer category) {
        if (category == null) {
            return "未知";
        }

        return switch (category) {
            case 1 -> "公办公营";
            case 2 -> "公办民营";
            case 3 -> "民营";
            default -> "未知";
        };
    }

    private String getMatchLevel(Double score) {
        if (score == null) {
            return "unknown";
        }

        if (score >= 0.85) {
            return "excellent";
        }

        if (score >= 0.70) {
            return "good";
        }

        if (score >= 0.55) {
            return "medium";
        }

        return "weak";
    }

    private String getMatchLevelText(Double score) {
        if (score == null) {
            return "暂无评分";
        }

        if (score >= 0.85) {
            return "高度推荐";
        }

        if (score >= 0.70) {
            return "推荐";
        }

        if (score >= 0.55) {
            return "可作为备选";
        }

        return "匹配度较低";
    }

    private boolean containsAny(String value, String... keywords) {
        if (value == null || value.isBlank()) {
            return false;
        }

        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private Double round(Double value, int scale) {
        if (value == null) {
            return null;
        }

        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    private record WeightConfig(
            Double medicalWeight,
            Double lifeWeight,
            Double greenWeight,
            Double visitWeight
    ) {
    }
}