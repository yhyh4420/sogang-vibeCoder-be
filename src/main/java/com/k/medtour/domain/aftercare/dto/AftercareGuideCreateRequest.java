package com.k.medtour.domain.aftercare.dto;


import java.util.Map;

public record AftercareGuideCreateRequest(
        Long journeyId,
        String title,
        String content,
        Map<String, Object> instructions
) {
}
