package com.k.medtour.domain.patient.repository;

import com.k.medtour.domain.patient.entity.MedicalQuestionnaire;

import java.util.Optional;

public interface MedicalQuestionnaireRepository {

    MedicalQuestionnaire save(MedicalQuestionnaire questionnaire);

    Optional<MedicalQuestionnaire> findById(Long id);

    Optional<MedicalQuestionnaire> findByMemberIdAndDeletedAtIsNull(Long memberId);

    boolean existsByMemberIdAndDeletedAtIsNull(Long memberId);
}
