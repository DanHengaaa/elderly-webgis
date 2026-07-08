package com.hhu.elderly.ai.controller;

import com.hhu.elderly.ai.dto.AiChatRequest;
import com.hhu.elderly.ai.dto.AiChatResponse;
import com.hhu.elderly.ai.service.AiAssistantService;
import com.hhu.elderly.ai.service.LlmClientService;
import org.springframework.web.bind.annotation.*;
import com.hhu.elderly.ai.dto.LlmProviderOption;
import java.util.List;
@RestController
@RequestMapping("/api/ai-assistant")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final LlmClientService llmClientService;

    public AiAssistantController(
            AiAssistantService aiAssistantService,
            LlmClientService llmClientService
    ) {
        this.aiAssistantService = aiAssistantService;
        this.llmClientService = llmClientService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "ai assistant controller ok";
    }

    @GetMapping("/providers")
    public List<LlmProviderOption> providers() {
        return llmClientService.getProviderOptions();
    }

    @GetMapping("/llm-test")
    public String testLlm() {
        return llmClientService.testConnection();
    }

    @PostMapping("/chat")
    public AiChatResponse chat(@RequestBody AiChatRequest request) {
        return aiAssistantService.chat(request);
    }
}