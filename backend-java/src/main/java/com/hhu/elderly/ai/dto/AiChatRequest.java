package com.hhu.elderly.ai.dto;

import java.util.List;

public record AiChatRequest(
        String message,

        /**
         * AI 服务商 ID。
         * 当前默认 deepseek，后续可扩展 qwen、zhipu、openai 等。
         */
        String providerId,

        /**
         * 最近几轮对话上下文。
         */
        List<AiHistoryMessage> history,

        /**
         * 上一轮推荐结果，用于“比较前3家机构”等多轮追问。
         */
        List<AiContextRecommendationItem> previousRecommendations,

        /**
         * 探视起点，可为空。
         */
        Double startLon,
        Double startLat,
        String startName
) {
}