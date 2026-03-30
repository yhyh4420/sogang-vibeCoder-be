package com.k.medtour.domain.aftercare.repository;

import com.k.medtour.domain.aftercare.entity.AftercareGuide;

import java.util.Optional;

public interface AftercareGuideRepository {

    AftercareGuide save(AftercareGuide guide);

    Optional<AftercareGuide> findById(Long id);

    Optional<AftercareGuide> findByJourneyIdAndDeletedAtIsNull(Long journeyId);

    boolean existsByJourneyIdAndDeletedAtIsNull(Long journeyId);
}
