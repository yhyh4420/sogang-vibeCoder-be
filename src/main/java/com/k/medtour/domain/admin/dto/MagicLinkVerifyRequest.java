package com.k.medtour.domain.admin.dto;


import java.time.LocalDate;

public record MagicLinkVerifyRequest(
        String token,

        LocalDate birthDate
) {
}
