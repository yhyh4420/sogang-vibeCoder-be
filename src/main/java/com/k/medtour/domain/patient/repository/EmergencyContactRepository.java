package com.k.medtour.domain.patient.repository;

import com.k.medtour.domain.patient.entity.EmergencyContact;

import java.util.List;
import java.util.Optional;

public interface EmergencyContactRepository {

    EmergencyContact save(EmergencyContact emergencyContact);

    Optional<EmergencyContact> findById(Long id);

    List<EmergencyContact> findAllByMemberIdAndDeletedAtIsNull(Long memberId);

    Optional<EmergencyContact> findByIdAndMemberIdAndDeletedAtIsNull(Long id, Long memberId);
}
