package com.k.medtour.domain.admin.repository;

import com.k.medtour.domain.admin.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findById(Long id);

    Optional<RefreshToken> findByToken(String token);

    void revokeAllByMemberId(Long memberId);
}
