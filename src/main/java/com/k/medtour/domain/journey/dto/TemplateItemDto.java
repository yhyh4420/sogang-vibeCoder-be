package com.k.medtour.domain.journey.dto;

import com.k.medtour.domain.journey.entity.JourneyTemplateItem;
import com.k.medtour.domain.journey.enums.ScheduleItemType;

import java.util.List;

public record TemplateItemDto(
        Long id,
        Integer dayOffset,
        String timeOffset,
        String title,
        ScheduleItemType type,
        String description,
        Integer durationMinutes,
        LocationDto location,
        List<String> requiredStaff,
        Integer order
) {
    public static TemplateItemDto from(JourneyTemplateItem entity) {
        return new TemplateItemDto(
                entity.getId(),
                entity.getDayOffset(),
                entity.getTimeOffset(),
                entity.getTitle(),
                entity.getType(),
                entity.getDescription(),
                entity.getDurationMinutes(),
                LocationDto.from(entity.getLocation()),
                entity.getRequiredStaff(),
                entity.getSortOrder()
        );
    }
}
