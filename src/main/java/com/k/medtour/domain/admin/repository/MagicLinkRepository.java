package com.k.medtour.domain.admin.repository;

import com.k.medtour.domain.admin.entity.MagicLink;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface MagicLinkRepository {

    MagicLink save(MagicLink magicLink);

    Optional<MagicLink> findById(Long id);

    Optional<MagicLink> findByToken(UUID token);

    long countRecentByTargetEmail(String target, LocalDateTime since);

    long countRecentByTargetPhone(String target, LocalDateTime since);
}
