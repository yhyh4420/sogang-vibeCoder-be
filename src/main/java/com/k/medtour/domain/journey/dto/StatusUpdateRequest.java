package com.k.medtour.domain.journey.dto;

import com.k.medtour.domain.journey.enums.ScheduleItemStatus;

public record StatusUpdateRequest(
        ScheduleItemStatus status,
        String note
) {
}
