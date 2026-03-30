package com.k.medtour.domain.journey.repository;

import com.k.medtour.domain.journey.entity.StaffAssignment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StaffAssignmentRepository {

    StaffAssignment save(StaffAssignment assignment);

    Optional<StaffAssignment> findById(Long id);

    List<StaffAssignment> findByScheduleItemId(Long scheduleItemId);

    List<StaffAssignment> findByStaffIdAndDate(Long staffId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    boolean existsByScheduleItemIdAndStaffId(Long scheduleItemId, Long staffId);

    Optional<StaffAssignment> findByScheduleItemIdAndStaffId(Long scheduleItemId, Long staffId);

    boolean existsByJourneyIdAndStaffId(Long journeyId, Long staffId);
}
