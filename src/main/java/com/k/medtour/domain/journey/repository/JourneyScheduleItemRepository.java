package com.k.medtour.domain.journey.repository;

import com.k.medtour.domain.journey.entity.JourneyScheduleItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JourneyScheduleItemRepository {

    JourneyScheduleItem save(JourneyScheduleItem item);

    Optional<JourneyScheduleItem> findById(Long id);

    void deleteById(Long id);

    List<JourneyScheduleItem> findByJourneyIdOrderByScheduledAtAsc(Long journeyId);

    List<JourneyScheduleItem> findByJourneyIdAndDate(Long journeyId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    Optional<JourneyScheduleItem> findByIdAndJourneyId(Long id, Long journeyId);

    long countByScheduledAtBetweenAndDeletedAtIsNull(LocalDateTime start, LocalDateTime end);
}
