package com.k.medtour.domain.patient.repository;

import com.k.medtour.domain.patient.entity.PatientPassport;

import java.util.Optional;

public interface PatientPassportRepository {

    PatientPassport save(PatientPassport passport);

    Optional<PatientPassport> findById(Long id);

    Optional<PatientPassport> findByMemberIdAndDeletedAtIsNull(Long memberId);

    boolean existsByMemberIdAndDeletedAtIsNull(Long memberId);
}
