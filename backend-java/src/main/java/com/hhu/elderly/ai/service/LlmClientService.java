package com.hhu.elderly.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhu.elderly.ai.dto.AiContextRecommendationItem;
import com.hhu.elderly.ai.dto.AiHistoryMessage;
import com.hhu.elderly.ai.dto.LlmProviderOption;
import com.hhu.elderly.recommendation.dto.RecommendationRequest;
import com.hhu.elderly.recommendation.dto.RecommendationResponse;
import com.hhu.elderly.recommendation.dto.RecommendationResponse.RecommendationItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.hhu.elderly.community.dto.CommunityInstitutionPostBrief;


@Service
public class LlmClientService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${llm.enabled:false}")
    private Boolean enabled;

    @Value("${llm.base-url:}")
    private String baseUrl;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.model:deepseek-chat}")
    private String model;

    public LlmClientService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    public List<LlmProviderOption> getProviderOptions() {
        return List.of(
                new LlmProviderOption(
                        "deepseek",
                        "DeepSeek",
                        model,
                        isConfigured(),
                        "DeepSeek 官方 API，用于养老机构推荐解释、对比分析和多轮问答"
                )
        );
    }

    public String getProviderName(String providerId) {
        if (isDeepSeek(providerId)) {
            return "DeepSeek";
        }

        return "系统规则引擎";
    }

    public String getModelName(String providerId) {
        if (isDeepSeek(providerId)) {
            return model;
        }

        return "Rule-MVP";
    }

    public String generateRecommendationAnswer(
            String userMessage,
            RecommendationRequest request,
            RecommendationResponse response,
            String providerId,
            List<AiHistoryMessage> history
    ) {
        if (!isDeepSeek(providerId) || !isConfigured()) {
            return null;
        }

        if (response == null || response.items() == null || response.items().isEmpty()) {
            return null;
        }

        try {
            String systemPrompt = """
                    你是一个养老机构智能伴诊助手，服务于“养老资源空间适配评估与智慧决策 WebGIS 平台”。

                    你的任务：
                    根据系统提供的真实推荐结果，向用户解释为什么推荐这些养老机构。

                    严格要求：
                    1. 只能基于系统提供的推荐机构回答，不得编造机构、价格、床位、医院距离或评分。
                    2. 不要说自己替代医生、护理评估师或政府评级。
                    3. 推荐理由要结合预算、护理需求、医疗资源、生活便利、绿色休闲和探视可达性。
                    4. 语气要专业、温和、清楚，像养老顾问。
                    5. 可以使用 Markdown，例如 **重点标题**、编号列表，但不要写成长篇论文。
                    6. 如果价格为 0、未知或空值，不要说“便宜”，要说“系统暂无完整价格数据”。
                    7. 如果床位未知，不要说“一定有床位”，要提醒用户进一步核实。
                    8. 如果系统识别需求中的“探视起点”非空，并且用户问题包含“附近、周边、离某地近、学校附近、家附近”等表达，应把探视起点视为本轮空间偏好，不要说系统没有纳入该地点。
                                               9. 如果用户本轮提出了新地点、新区域或新推荐需求，应优先依据本轮信息，不要被上一轮推荐结果限制。
                                               10. 最后提醒用户点击“查看详情”进一步核对机构信息。
                    """;

            String userPrompt = buildRecommendationPrompt(userMessage, request, response, history);

            return callDeepSeek(systemPrompt, userPrompt, 1400);
        } catch (Exception exception) {
            System.err.println("DeepSeek 推荐解释生成失败：" + exception.getMessage());
            return null;
        }
    }

    public String generateComparisonAnswer(
            String userMessage,
            List<AiHistoryMessage> history,
            List<AiContextRecommendationItem> previousRecommendations,
            String providerId
    ) {
        if (!isDeepSeek(providerId) || !isConfigured()) {
            return null;
        }

        if (previousRecommendations == null || previousRecommendations.isEmpty()) {
            return null;
        }

        try {
            String systemPrompt = """
                    你是一个养老机构智能伴诊助手，擅长基于真实推荐结果做机构对比。

                    你的任务：
                    根据上一轮系统推荐的养老机构，从用户关心的角度进行横向比较。

                    严格要求：
                    1. 只能比较系统提供的机构，不得编造新机构。
                    2. 重点比较前 3 家机构。
                    3. 比较维度包括：预算匹配、医疗资源、生活便利、绿色环境、探视可达性、床位/数据完整性。
                    4. 如果价格为 0 或床位未知，要明确说“系统暂无完整价格/床位数据”，不能误导。
                    5. 最后给出“更适合哪类老人/家庭”的选择建议。
                    6. 使用清晰 Markdown 输出，可以用 **标题** 和编号列表。
                    """;

            String userPrompt = buildComparisonPrompt(userMessage, history, previousRecommendations);

            return callDeepSeek(systemPrompt, userPrompt, 1600);
        } catch (Exception exception) {
            System.err.println("DeepSeek 机构对比生成失败：" + exception.getMessage());
            return null;
        }
    }

    public String generateGeneralAnswer(
            String userMessage,
            List<AiHistoryMessage> history,
            List<AiContextRecommendationItem> previousRecommendations,
            String providerId
    ) {
        if (!isDeepSeek(providerId) || !isConfigured()) {
            return null;
        }

        try {
            String systemPrompt = """
                你是“养老资源空间适配评估与智慧决策 WebGIS 平台”的 AI 伴诊助手。

                你的能力包括：
                1. 与用户进行自然对话，解释你是谁、能做什么、当前使用什么模型。
                2. 帮助用户梳理养老机构选择需求，包括年龄、护理等级、预算、区域、医疗资源、探视便利性等。
                3. 基于平台上一轮推荐结果，继续回答用户追问。
                4. 当用户询问“用了什么模型”“你是谁”“怎么推荐”时，要清楚解释：
                   - 当前语言生成由所选大模型完成；
                   - 养老机构结果来自平台智能推荐算法和 GIS 空间分析；
                   - 你不会凭空编造机构。
                5. 如果用户只是普通寒暄，也要自然回复，不要机械重复示例问题。
                6. 不要冒充医生、政府部门或护理评估师。
                7. 回答要简洁、自然、专业，适合养老服务平台答辩和演示。
                """;

            String userPrompt = buildGeneralPrompt(
                    userMessage,
                    history,
                    previousRecommendations,
                    providerId
            );

            return callDeepSeek(systemPrompt, userPrompt, 1200);
        } catch (Exception exception) {
            System.err.println("DeepSeek 通用对话生成失败：" + exception.getMessage());
            return null;
        }
    }

    private String buildGeneralPrompt(
            String userMessage,
            List<AiHistoryMessage> history,
            List<AiContextRecommendationItem> previousRecommendations,
            String providerId
    ) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("用户本轮问题", userMessage);
        payload.put("当前AI服务商", getProviderName(providerId));
        payload.put("当前模型", getModelName(providerId));
        payload.put("最近对话上下文", buildBriefHistory(history));

        if (previousRecommendations != null && !previousRecommendations.isEmpty()) {
            payload.put("上一轮推荐结果", previousRecommendations.stream().limit(5).toList());
        } else {
            payload.put("上一轮推荐结果", List.of());
        }

        return """
            请根据下面 JSON 信息回答用户。

            要求：
            1. 如果用户问你是谁，要说明你是养老服务 WebGIS 平台的 AI 伴诊助手。
            2. 如果用户问用了什么大模型，要回答当前选择的 AI 服务商和模型。
            3. 如果用户问推荐依据，要说明推荐机构来自平台智能推荐算法和 GIS 空间分析，大模型负责解释和对话。
            4. 如果用户没有提供养老需求，可以自然引导用户补充年龄、护理需求、预算、区域和偏好。
            5. 如果存在上一轮推荐结果，可以结合上一轮结果继续回答。
            6. 不要编造数据库中没有的养老机构。
            7. 不要每次都机械重复同一句示例问题。

            JSON：
            """ + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
    }

    public String generateCommunityOpinionSummary(
            String institutionName,
            Integer postCount,
            Object avgRating,
            List<CommunityInstitutionPostBrief> recentPosts,
            String providerId
    ) {
        if (!isDeepSeek(providerId) || !isConfigured()) {
            return null;
        }

        if (recentPosts == null || recentPosts.isEmpty()) {
            return null;
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("机构名称", institutionName);
            payload.put("社区笔记数量", postCount);
            payload.put("社区平均评分", avgRating);
            payload.put("近期社区笔记", recentPosts);

            String systemPrompt = """
                你是养老服务平台的社区口碑分析助手。

                你的任务：
                根据用户发布的真实社区笔记，生成某养老机构的社区口碑摘要。

                严格要求：
                1. 只能基于提供的社区笔记进行总结，不得编造新评价。
                2. 如果笔记数量少，要明确提醒“当前社区样本有限”。
                3. 总结应包括：用户关注点、正向反馈、需要核实的信息、适合人群。
                4. 不要替代官方评级或护理评估。
                5. 语言要简洁、专业、适合显示在机构详情页。
                """;

            String userPrompt = """
                请根据下面 JSON 生成一段机构社区口碑摘要。

                输出结构：
                1. **总体口碑**
                2. **用户主要关注点**
                3. **需要进一步核实的信息**
                4. **适合参考的人群**

                JSON：
                """ + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

            return callDeepSeek(systemPrompt, userPrompt, 1000);
        } catch (Exception exception) {
            System.err.println("DeepSeek 社区口碑摘要生成失败：" + exception.getMessage());
            return null;
        }
    }

    public String testConnection() {
        if (!isConfigured()) {
            return "DeepSeek 未启用或配置不完整，请检查 llm.enabled、llm.base-url、llm.api-key、llm.model。";
        }

        try {
            String result = callDeepSeek(
                    "你是一个接口连通性测试助手。",
                    "请只回复一句话：DeepSeek API 连接成功，随时待命。",
                    100
            );

            if (result == null || result.isBlank()) {
                return "DeepSeek API 返回为空。";
            }

            return result;
        } catch (Exception exception) {
            return "DeepSeek API 测试失败：" + exception.getMessage();
        }
    }

    private String callDeepSeek(
            String systemPrompt,
            String userPrompt,
            int maxTokens
    ) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("stream", false);
        body.put("temperature", 0.35);
        body.put("max_tokens", maxTokens);

        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(Map.of(
                "role", "system",
                "content", systemPrompt
        ));

        messages.add(Map.of(
                "role", "user",
                "content", userPrompt
        ));

        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(cleanApiKey(apiKey));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        String url = normalizeBaseUrl(baseUrl) + "/chat/completions";

        ResponseEntity<String> result = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        if (!result.getStatusCode().is2xxSuccessful()) {
            System.err.println("DeepSeek API 状态码异常：" + result.getStatusCode());
            return null;
        }

        if (result.getBody() == null || result.getBody().isBlank()) {
            System.err.println("DeepSeek API 返回为空");
            return null;
        }

        JsonNode root = objectMapper.readTree(result.getBody());

        JsonNode contentNode = root
                .path("choices")
                .path(0)
                .path("message")
                .path("content");

        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            System.err.println("DeepSeek API 未返回有效 content：" + result.getBody());
            return null;
        }

        return contentNode.asText();
    }

    private String buildRecommendationPrompt(
            String userMessage,
            RecommendationRequest request,
            RecommendationResponse response,
            List<AiHistoryMessage> history
    ) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("用户本轮问题", userMessage);
        payload.put("最近对话上下文", buildBriefHistory(history));

        Map<String, Object> parsedNeed = new LinkedHashMap<>();
        parsedNeed.put("年龄", request.age());
        parsedNeed.put("护理需求", request.careLevel());
        parsedNeed.put("预算下限", request.budgetMin());
        parsedNeed.put("预算上限", request.budgetMax());
        parsedNeed.put("意向区县", request.preferredDistrict());
        parsedNeed.put("探视起点", request.startName());

        payload.put("系统识别需求", parsedNeed);
        payload.put("系统推荐结果", buildBriefRecommendationItems(response));

        return """
                请根据下面 JSON 信息，生成一段给用户看的养老机构推荐说明。

                输出结构建议：
                1. **系统识别到的需求**
                2. **重点机构推荐理由**
                3. **需要进一步核实的信息**
                4. **下一步建议**

                JSON：
                """ + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
    }

    private String buildComparisonPrompt(
            String userMessage,
            List<AiHistoryMessage> history,
            List<AiContextRecommendationItem> previousRecommendations
    ) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("用户本轮问题", userMessage);
        payload.put("最近对话上下文", buildBriefHistory(history));
        payload.put("上一轮推荐结果", previousRecommendations.stream().limit(5).toList());

        return """
                用户正在基于上一轮推荐结果追问，请比较前 3 家机构。

                输出结构建议：
                1. **整体结论**
                2. **前3家机构横向对比**
                3. **不同家庭选择建议**
                4. **需要进一步核实的信息**

                JSON：
                """ + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
    }

    private List<Map<String, Object>> buildBriefRecommendationItems(
            RecommendationResponse response
    ) {
        List<Map<String, Object>> list = new ArrayList<>();

        for (RecommendationItem item : response.items().stream().limit(5).toList()) {
            Map<String, Object> map = new LinkedHashMap<>();

            map.put("机构ID", item.id());
            map.put("机构名称", item.name());
            map.put("区县", item.district());
            map.put("地址", item.address());
            map.put("机构性质", item.institutionCategoryText());
            map.put("月费", item.monthlyFeeBase());
            map.put("可用床位", item.availableBeds());
            map.put("总床位", item.totalBeds());
            map.put("推荐分", item.finalScore());
            map.put("推荐等级", item.matchLevelText());
            map.put("预算匹配分", item.budgetScore());
            map.put("医疗资源分", item.medicalScore());
            map.put("生活便利分", item.lifeScore());
            map.put("绿色休闲分", item.greenScore());
            map.put("探视可达分", item.visitScore());
            map.put("探视耗时分钟", item.estimatedVisitMinutes());
            map.put("系统推荐理由", item.reasons());

            list.add(map);
        }

        return list;
    }

    private List<Map<String, String>> buildBriefHistory(List<AiHistoryMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        return history.stream()
                .filter(item -> item != null && hasText(item.content()))
                .skip(Math.max(0, history.size() - 8))
                .map(item -> Map.of(
                        "role", item.role() == null ? "unknown" : item.role(),
                        "content", item.content()
                ))
                .toList();
    }

    private boolean isConfigured() {
        return Boolean.TRUE.equals(enabled)
                && hasText(baseUrl)
                && hasText(apiKey)
                && hasText(model);
    }

    private boolean isDeepSeek(String providerId) {
        return providerId == null
                || providerId.isBlank()
                || "deepseek".equalsIgnoreCase(providerId.trim());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeBaseUrl(String value) {
        if (value == null) {
            return "";
        }

        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }

    private String cleanApiKey(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();

        if (cleaned.startsWith("Bearer ")) {
            cleaned = cleaned.substring("Bearer ".length()).trim();
        }

        if (
                (cleaned.startsWith("\"") && cleaned.endsWith("\""))
                        || (cleaned.startsWith("'") && cleaned.endsWith("'"))
        ) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        return cleaned;
    }
}