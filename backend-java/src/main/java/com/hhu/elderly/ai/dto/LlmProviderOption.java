package com.hhu.elderly.ai.dto;

public record LlmProviderOption(
        String id,
        String name,
        String model,
        Boolean enabled,
        String description
) {
}