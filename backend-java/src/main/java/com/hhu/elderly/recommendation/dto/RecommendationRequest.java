package com.hhu.elderly.recommendation.dto;

import java.math.BigDecimal;

public record RecommendationRequest(
        String elderlyName,
        Integer age,

        /**
         * 自理能力 / 护理需求：
         * selfCare    自理
         * semiCare    半失能
         * nursing     失能护理
         * dementia    认知症照护
         */
        String careLevel,

        BigDecimal budgetMin,
        BigDecimal budgetMax,

        String preferredDistrict,

        /**
         * 机构性质：
         * 1 公办公营
         * 2 公办民营
         * 3 民营
         */
        Integer preferredCategory,

        String gradeLevel,

        Boolean hasAvailableBeds,

        /**
         * 探视起点，可为空。
         * 如果传入，就计算探视可达性分。
         */
        Double startLon,
        Double startLat,
        String startName,

        /**
         * 用户偏好权重，建议前端传 1-5。
         */
        Double medicalPriority,
        Double lifePriority,
        Double greenPriority,
        Double visitPriority,

        Integer limit
) {
}