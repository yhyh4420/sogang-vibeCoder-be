package com.k.medtour.domain.aftercare.dto;


public record StaffReportCreateRequest(
        Long journeyId,
        String reportContent,
        Double workHours
) {
}
