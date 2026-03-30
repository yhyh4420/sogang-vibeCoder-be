package com.k.medtour.domain.journey.dto;

import com.k.medtour.domain.journey.enums.ScheduleItemType;

import java.time.LocalDateTime;
import java.util.List;

public record ScheduleItemCreateRequest(
        LocalDateTime scheduledAt,
        String title,
        ScheduleItemType type,
        String description,
        Integer durationMinutes,
        LocationDto location,
        List<String> requiredStaff
) {
}
