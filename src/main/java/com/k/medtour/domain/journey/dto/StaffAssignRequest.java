package com.k.medtour.domain.journey.dto;

import java.util.List;

public record StaffAssignRequest(
        Long staffId,
        List<Long> scheduleItemIds
) {
}
