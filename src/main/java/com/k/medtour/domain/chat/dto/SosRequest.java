package com.k.medtour.domain.chat.dto;

public record SosRequest(
        String message,

        String roomId,

        Long journeyId
) {
}
