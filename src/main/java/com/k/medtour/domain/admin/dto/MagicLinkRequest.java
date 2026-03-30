package com.k.medtour.domain.admin.dto;


public record MagicLinkRequest(
        String target,

        String targetType,

        String role,

        String language
) {
}
