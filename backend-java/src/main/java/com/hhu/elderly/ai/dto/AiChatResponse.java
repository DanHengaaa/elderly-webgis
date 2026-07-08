package com.hhu.elderly.ai.dto;

import com.hhu.elderly.recommendation.dto.RecommendationResponse;

import java.util.List;

public record AiChatResponse(
        String intent,
        String answer,
        RecommendationResponse recommendations,
        List<String> suggestions,

        /**
         * 例如 DeepSeek / 系统规则引擎。
         */
        String providerName,

        /**
         * 例如 deepseek-chat / Rule-MVP。
         */
        String model,

        /**
         * 前端展示用，例如“由 DeepSeek · deepseek-chat 生成，基于平台推荐算法与 GIS 分析结果。”
         */
        String answerSource,

        /**
         * 生成时间。
         */
        String generatedAt
) {
}