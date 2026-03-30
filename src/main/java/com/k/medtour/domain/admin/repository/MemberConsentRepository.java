package com.k.medtour.domain.admin.repository;

import com.k.medtour.domain.admin.entity.MemberConsent;

import java.util.Optional;

public interface MemberConsentRepository {

    MemberConsent save(MemberConsent memberConsent);

    Optional<MemberConsent> findById(Long id);

    Optional<MemberConsent> findTopByMemberIdOrderByConsentedAtDesc(Long memberId);
}
