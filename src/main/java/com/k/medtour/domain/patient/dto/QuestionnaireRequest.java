package com.k.medtour.domain.patient.dto;

import com.k.medtour.domain.patient.enums.BloodType;

import java.util.List;

public record QuestionnaireRequest(
        BloodType bloodType,

        Double height,

        Double weight,

        List<String> allergies,

        List<String> currentMedications,

        List<String> pastSurgeries,

        List<String> chronicConditions,

        String additionalNotes
) {
}
