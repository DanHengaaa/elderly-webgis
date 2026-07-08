package com.hhu.elderly.recommendation.controller;

import com.hhu.elderly.recommendation.dto.RecommendationRequest;
import com.hhu.elderly.recommendation.dto.RecommendationResponse;
import com.hhu.elderly.recommendation.service.RecommendationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/ping")
    public String ping() {
        return "recommendation controller ok";
    }

    @PostMapping
    public RecommendationResponse recommend(
            @RequestBody RecommendationRequest request
    ) {
        return recommendationService.recommend(request);
    }
}