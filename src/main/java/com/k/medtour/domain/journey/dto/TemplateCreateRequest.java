package com.k.medtour.domain.journey.dto;

import com.k.medtour.domain.journey.enums.TemplateCategory;

import java.util.List;

public record TemplateCreateRequest(
        String name,
        TemplateCategory category,
        Integer durationDays,
        List<TemplateItemDto> items
) {
}
