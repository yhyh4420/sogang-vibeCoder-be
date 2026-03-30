package com.k.medtour.domain.patient.dto;

import com.k.medtour.domain.patient.enums.Gender;
import com.k.medtour.domain.patient.enums.PassportInputType;

import java.time.LocalDate;

public record PassportRequest(
        String passportNumber,

        String fullName,

        String nationality,

        LocalDate birthDate,

        LocalDate expiryDate,

        Gender gender,

        PassportInputType inputType,

        Long fileId,

        Double ocrConfidence
) {
}
