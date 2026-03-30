package com.k.medtour.domain.patient.dto;


public record EmergencyContactRequest(
        String name,

        String relationship,

        String phone,

        String email,

        Boolean isPrimary
) {
}
