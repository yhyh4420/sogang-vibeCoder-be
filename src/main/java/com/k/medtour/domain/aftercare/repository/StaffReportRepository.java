package com.k.medtour.domain.aftercare.repository;

import com.k.medtour.domain.aftercare.entity.StaffReport;

import java.util.Optional;

public interface StaffReportRepository {

    StaffReport save(StaffReport staffReport);

    Optional<StaffReport> findById(Long id);

    boolean existsByJourneyIdAndStaffIdAndDeletedAtIsNull(Long journeyId, Long staffId);

    Optional<StaffReport> findByJourneyIdAndStaffIdAndDeletedAtIsNull(Long journeyId, Long staffId);
}
