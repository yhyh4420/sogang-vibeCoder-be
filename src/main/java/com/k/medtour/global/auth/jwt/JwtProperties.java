package com.k.medtour.global.auth.jwt;

/**
 * JWT configuration properties.
 * No longer uses Spring @ConfigurationProperties; values are set via AppConfig.
 */
public record JwtProperties(
        String secret,
        long accessTokenExpiration,
        long refreshTokenExpiration
) {
}
