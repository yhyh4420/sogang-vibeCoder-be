package com.k.medtour.domain.journey.dto;


import java.time.LocalDate;

public record JourneyCreateRequest(
        Long patientId,
        Long templateId,
        LocalDate startDate,
        String title,
        String notes
) {
}
