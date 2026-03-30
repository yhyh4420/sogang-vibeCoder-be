package com.k.medtour.domain.journey.repository;

import com.k.medtour.domain.journey.entity.Journey;
import com.k.medtour.domain.journey.enums.JourneyStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JourneyRepository {

    Journey save(Journey journey);

    Optional<Journey> findById(Long id);

    Optional<Journey> findByIdAndDeletedAtIsNull(Long id);

    List<Journey> findAllByFilters(JourneyStatus status, Long patientId,
                                    LocalDate startDateFrom, LocalDate startDateTo,
                                    int page, int size);

    long countByFilters(JourneyStatus status, Long patientId,
                        LocalDate startDateFrom, LocalDate startDateTo);

    boolean existsByPatientIdAndStatusIn(Long patientId, List<JourneyStatus> statuses);

    List<Journey> findByPatientIdAndStatusIn(Long patientId, List<JourneyStatus> statuses);
}
