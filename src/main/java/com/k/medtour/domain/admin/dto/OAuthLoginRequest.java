package com.k.medtour.domain.admin.dto;


public record OAuthLoginRequest(
        String idToken,

        DeviceInfo deviceInfo
) {
    public record DeviceInfo(
            String platform,

            String language
    ) {
    }
}
