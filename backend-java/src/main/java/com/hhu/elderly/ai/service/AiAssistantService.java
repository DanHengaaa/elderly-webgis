package com.hhu.elderly.ai.service;

import com.hhu.elderly.ai.dto.AiChatRequest;
import com.hhu.elderly.ai.dto.AiChatResponse;
import com.hhu.elderly.ai.dto.AiContextRecommendationItem;
import com.hhu.elderly.recommendation.dto.RecommendationRequest;
import com.hhu.elderly.recommendation.dto.RecommendationResponse;
import com.hhu.elderly.recommendation.dto.RecommendationResponse.RecommendationItem;
import com.hhu.elderly.recommendation.service.RecommendationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiAssistantService {

    private static final List<String> NANJING_DISTRICTS = List.of(
            "玄武区",
            "秦淮区",
            "建邺区",
            "鼓楼区",
            "浦口区",
            "栖霞区",
            "雨花台区",
            "江宁区",
            "六合区",
            "溧水区",
            "高淳区"
    );

    private final RecommendationService recommendationService;
    private final LlmClientService llmClientService;

    public AiAssistantService(
            RecommendationService recommendationService,
            LlmClientService llmClientService
    ) {
        this.recommendationService = recommendationService;
        this.llmClientService = llmClientService;
    }

    public AiChatResponse chat(AiChatRequest request) {
        String message = request.message() == null ? "" : request.message().trim();
        String providerId = request.providerId() == null || request.providerId().isBlank()
                ? "deepseek"
                : request.providerId().trim();

        if (message.isBlank()) {
            return buildResponse(
                    "empty",
                    "请告诉我老人的基本情况、预算、护理需求或希望优先考虑的因素，例如：老人78岁、半失能、预算7000、希望离医院近。",
                    null,
                    defaultSuggestions(),
                    providerId,
                    false
            );
        }

        if (isCompareIntent(message)) {
            return handleComparison(request, message, providerId);
        }

        if (isRecommendationIntent(message)) {
            RecommendationRequest recommendationRequest = buildRecommendationRequest(message, request);

            RecommendationResponse recommendationResponse =
                    recommendationService.recommend(recommendationRequest);

            String answer = buildRecommendationAnswer(message, recommendationRequest, recommendationResponse);

            String llmAnswer = llmClientService.generateRecommendationAnswer(
                    message,
                    recommendationRequest,
                    recommendationResponse,
                    providerId,
                    request.history()
            );

            boolean usedLlm = llmAnswer != null && !llmAnswer.isBlank();

            if (usedLlm) {
                answer = llmAnswer;
            }

            return buildResponse(
                    "recommendation",
                    answer,
                    recommendationResponse,
                    List.of(
                            "帮我优先推荐医疗资源好的机构",
                            "帮我找预算更低的养老机构",
                            "帮我优先考虑探视方便",
                            "帮我比较前3家机构"
                    ),
                    providerId,
                    usedLlm
            );
        }

        return handleGeneralConversation(request, message, providerId);
    }


    private AiChatResponse handleComparison(
            AiChatRequest request,
            String message,
            String providerId
    ) {
        List<AiContextRecommendationItem> previousItems =
                request.previousRecommendations() == null
                        ? List.of()
                        : request.previousRecommendations();

        if (previousItems.isEmpty()) {
            return buildResponse(
                    "comparison",
                    "我还没有可用于对比的上一轮推荐结果。请先告诉我老人的年龄、预算、护理需求和偏好，我会先生成推荐列表，然后可以继续帮你比较前3家机构。",
                    null,
                    defaultSuggestions(),
                    providerId,
                    false
            );
        }

        String answer = buildRuleComparisonAnswer(previousItems);

        String llmAnswer = llmClientService.generateComparisonAnswer(
                message,
                request.history(),
                previousItems,
                providerId
        );

        boolean usedLlm = llmAnswer != null && !llmAnswer.isBlank();

        if (usedLlm) {
            answer = llmAnswer;
        }

        return buildResponse(
                "comparison",
                answer,
                null,
                List.of(
                        "哪一家更适合半失能老人",
                        "哪一家探视最方便",
                        "哪一家医疗资源更好",
                        "帮我重新按预算优先推荐"
                ),
                providerId,
                usedLlm
        );
    }

    private AiChatResponse handleGeneralConversation(
            AiChatRequest request,
            String message,
            String providerId
    ) {
        String llmAnswer = llmClientService.generateGeneralAnswer(
                message,
                request.history(),
                request.previousRecommendations(),
                providerId
        );

        boolean usedLlm = llmAnswer != null && !llmAnswer.isBlank();

        String answer = usedLlm
                ? llmAnswer
                : buildGeneralFallbackAnswer(message, providerId);

        return buildResponse(
                "general_chat",
                answer,
                null,
                buildGeneralSuggestions(request.previousRecommendations()),
                providerId,
                usedLlm
        );
    }

    private AiChatResponse buildResponse(
            String intent,
            String answer,
            RecommendationResponse recommendations,
            List<String> suggestions,
            String providerId,
            boolean usedLlm
    ) {
        String providerName = usedLlm
                ? llmClientService.getProviderName(providerId)
                : "系统规则引擎";

        String modelName = usedLlm
                ? llmClientService.getModelName(providerId)
                : "Rule-MVP";

        String answerSource = usedLlm
                ? "由 " + providerName + " · " + modelName + " 生成，基于平台智能推荐算法与 GIS 空间分析结果。"
                : "由系统规则引擎生成，基于平台智能推荐算法与 GIS 空间分析结果。";

        return new AiChatResponse(
                intent,
                answer,
                recommendations,
                suggestions,
                providerName,
                modelName,
                answerSource,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    private boolean isCompareIntent(String message) {
        return containsAny(
                message,
                "比较",
                "对比",
                "前3家",
                "前三家",
                "哪一家更好",
                "哪个更好",
                "区别",
                "优缺点",
                "这几家怎么选"
        );
    }

    private boolean isRecommendationIntent(String message) {
        return containsAny(
                message,
                "推荐",
                "找",
                "哪家",
                "养老院",
                "养老机构",
                "半失能",
                "失能",
                "自理",
                "认知症",
                "预算",
                "月费",
                "医院附近",
                "离医院近"
        );
    }

    private RecommendationRequest buildRecommendationRequest(
            String message,
            AiChatRequest aiRequest
    ) {
        Integer age = extractAge(message);
        BigDecimal budgetMax = extractBudgetMax(message);
        String careLevel = extractCareLevel(message);
        String district = extractDistrict(message);

        boolean hasStartPoint = aiRequest.startLon() != null && aiRequest.startLat() != null;

        Double medicalPriority = containsAny(message, "医疗", "医院", "三甲", "急救", "看病") ? 5.0 : 3.0;
        Double lifePriority = containsAny(message, "生活", "超市", "买菜", "便利", "配套") ? 5.0 : 3.0;
        Double greenPriority = containsAny(message, "环境", "安静", "公园", "绿地", "空气") ? 5.0 : 3.0;

        Double visitPriority = (
                hasStartPoint
                        || containsAny(message, "探视", "探望", "离家近", "家属", "交通方便", "附近", "周边", "周围", "学校附近", "单位附近", "家附近")
        ) ? 5.0 : 3.0;

        return new RecommendationRequest(
                "AI识别用户",
                age == null ? 78 : age,
                careLevel,
                BigDecimal.ZERO,
                budgetMax == null ? BigDecimal.valueOf(7000) : budgetMax,
                district == null ? "不限" : district,
                null,
                "不限",
                true,
                aiRequest.startLon(),
                aiRequest.startLat(),
                aiRequest.startName(),
                medicalPriority,
                lifePriority,
                greenPriority,
                visitPriority,
                5
        );
    }

    private String buildRecommendationAnswer(
            String originalMessage,
            RecommendationRequest request,
            RecommendationResponse response
    ) {
        if (response == null || response.items() == null || response.items().isEmpty()) {
            return """
                    我已经根据你的描述尝试生成推荐方案，但当前没有匹配到合适机构。

                    建议你放宽条件，例如：
                    1. 将意向区县改为“不限”
                    2. 将预算范围适当提高
                    3. 暂时不限制机构性质
                    4. 先查看综合推荐结果，再人工筛选
                    """;
        }

        StringBuilder builder = new StringBuilder();

        builder.append("**我根据你的描述识别到以下需求：**\n\n");
        builder.append("护理需求：").append(getCareLevelText(request.careLevel())).append("\n");
        builder.append("预算上限：").append(request.budgetMax()).append(" 元/月\n");
        builder.append("意向区县：").append(request.preferredDistrict()).append("\n");

        if (request.startLon() != null && request.startLat() != null) {
            builder.append("探视起点：").append(
                    request.startName() == null || request.startName().isBlank()
                            ? "已设置"
                            : request.startName()
            ).append("\n");
        }

        builder.append("\n**为你优先推荐以下机构：**\n\n");

        int index = 1;
        for (RecommendationItem item : response.items().stream().limit(3).toList()) {
            builder.append(index).append(". ").append(item.name()).append("\n");
            builder.append("   推荐分：").append(Math.round(item.finalScore() * 100)).append("，")
                    .append(item.matchLevelText()).append("\n");

            if (item.monthlyFeeBase() != null && item.monthlyFeeBase().compareTo(BigDecimal.ZERO) > 0) {
                builder.append("   参考月费：").append(item.monthlyFeeBase()).append(" 元/月\n");
            } else {
                builder.append("   参考月费：系统暂无完整价格数据\n");
            }

            if (item.estimatedVisitMinutes() != null) {
                builder.append("   探视耗时：约 ").append(item.estimatedVisitMinutes()).append(" 分钟\n");
            }

            if (item.reasons() != null && !item.reasons().isEmpty()) {
                builder.append("   推荐理由：").append(String.join("；", item.reasons())).append("\n");
            }

            builder.append("\n");
            index++;
        }

        builder.append("你可以点击右侧推荐卡片中的“查看详情”，进一步查看机构信息。");

        return builder.toString();
    }

    private String buildRuleComparisonAnswer(List<AiContextRecommendationItem> items) {
        StringBuilder builder = new StringBuilder();

        builder.append("**前3家机构对比结果如下：**\n\n");

        int index = 1;
        for (AiContextRecommendationItem item : items.stream().limit(3).toList()) {
            builder.append(index).append(". **").append(item.name()).append("**\n");
            builder.append("   推荐等级：").append(nullToText(item.matchLevelText())).append("\n");
            builder.append("   推荐分：").append(item.finalScore() == null ? "暂无" : Math.round(item.finalScore() * 100)).append("\n");
            builder.append("   区位：").append(nullToText(item.district())).append("\n");

            if (item.monthlyFeeBase() != null && item.monthlyFeeBase().compareTo(BigDecimal.ZERO) > 0) {
                builder.append("   月费：").append(item.monthlyFeeBase()).append(" 元/月\n");
            } else {
                builder.append("   月费：系统暂无完整价格数据\n");
            }

            if (item.estimatedVisitMinutes() != null) {
                builder.append("   探视耗时：约 ").append(item.estimatedVisitMinutes()).append(" 分钟\n");
            }

            if (item.reasons() != null && !item.reasons().isEmpty()) {
                builder.append("   主要优势：").append(String.join("；", item.reasons())).append("\n");
            }

            builder.append("\n");
            index++;
        }

        builder.append("建议优先结合家属探视频率、老人护理等级和价格信息完整性，再进一步查看详情页核实。");

        return builder.toString();
    }

    private Integer extractAge(String message) {
        Matcher matcher = Pattern.compile("(\\d{2,3})\\s*岁").matcher(message);

        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private BigDecimal extractBudgetMax(String message) {
        List<Pattern> patterns = List.of(
                Pattern.compile("预算[^0-9]{0,8}(\\d{3,5})"),
                Pattern.compile("月费[^0-9]{0,8}(\\d{3,5})"),
                Pattern.compile("(\\d{3,5})\\s*元"),
                Pattern.compile("(\\d{3,5})\\s*以内")
        );

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(message);

            if (matcher.find()) {
                try {
                    return BigDecimal.valueOf(Long.parseLong(matcher.group(1)));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }

        return null;
    }

    private String extractCareLevel(String message) {
        if (containsAny(message, "认知症", "阿尔茨海默", "老年痴呆", "记忆障碍")) {
            return "dementia";
        }

        if (containsAny(message, "半失能", "半自理", "需要协助", "行动不便")) {
            return "semiCare";
        }

        if (containsAny(message, "失能", "卧床", "不能自理", "护理")) {
            return "nursing";
        }

        if (containsAny(message, "自理", "能自理", "基本自理")) {
            return "selfCare";
        }

        return "semiCare";
    }

    private String extractDistrict(String message) {
        for (String district : NANJING_DISTRICTS) {
            if (message.contains(district)) {
                return district;
            }

            String shortName = district.replace("区", "");
            if (message.contains(shortName)) {
                return district;
            }
        }

        return null;
    }

    private String getCareLevelText(String careLevel) {
        if (careLevel == null) {
            return "未明确";
        }

        return switch (careLevel) {
            case "selfCare" -> "基本自理";
            case "semiCare" -> "半失能 / 需要协助";
            case "nursing" -> "失能护理";
            case "dementia" -> "认知症照护";
            default -> "未明确";
        };
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }

        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private String nullToText(String value) {
        if (value == null || value.isBlank()) {
            return "暂无";
        }

        return value;
    }

    private List<String> defaultSuggestions() {
        return List.of(
                "我妈妈78岁半失能，预算7000，希望离医院近",
                "帮我推荐鼓楼区适合失能护理的养老机构",
                "预算5000以内，想找生活便利的养老院",
                "家属经常探望，希望交通方便"
        );
    }

    private String buildGeneralFallbackAnswer(
            String message,
            String providerId
    ) {
        if (containsAny(message, "你是谁", "介绍一下自己", "你能做什么")) {
            return """
                我是本平台的 AI 伴诊助手，可以帮助你进行养老机构推荐、机构选择解释、医疗资源分析、探视便利性判断和机构对比。

                本平台的推荐结果来自智能推荐算法与 GIS 空间分析，AI 主要负责理解你的需求并用更清楚的方式解释结果。
                """;
        }

        if (containsAny(message, "大模型", "模型", "DeepSeek", "deepseek", "用了什么")) {
            return "当前 AI 对话默认接入 DeepSeek，推荐机构结果由平台智能推荐算法和 GIS 空间分析生成，DeepSeek 主要负责自然语言理解、解释和对比分析。";
        }

        if (containsAny(message, "怎么选", "如何选", "注意什么", "养老院怎么选")) {
            return """
                选择养老机构时，建议重点看以下方面：

                1. 护理需求是否匹配，例如自理、半失能、失能护理或认知症照护。
                2. 预算是否稳定可承受，需要关注月费、护理费、餐费和押金。
                3. 医疗资源是否便利，尤其是附近综合医院、三甲医院和急救响应条件。
                4. 家属探视是否方便，通勤时间会影响长期照护参与。
                5. 周边生活服务和环境质量是否适合长期居住。

                你可以告诉我老人的年龄、护理情况、预算和意向区域，我可以进一步帮你推荐和比较。
                """;
        }

        return "我可以继续帮你分析养老机构选择问题。你可以直接告诉我老人的年龄、护理需求、预算、意向区域，或者基于右侧推荐结果继续问我“帮我比较前3家机构”。";
    }

    private List<String> buildGeneralSuggestions(
            List<AiContextRecommendationItem> previousRecommendations
    ) {
        if (previousRecommendations != null && !previousRecommendations.isEmpty()) {
            return List.of(
                    "帮我比较前3家机构",
                    "哪一家更适合半失能老人",
                    "哪一家探视最方便",
                    "哪一家医疗资源更好"
            );
        }

        return List.of(
                "你用了什么大模型",
                "你能帮我做什么",
                "养老院应该怎么选",
                "我妈妈78岁半失能，预算7000，希望离医院近"
        );
    }


}