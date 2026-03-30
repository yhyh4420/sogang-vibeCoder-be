package com.k.medtour.domain.journey.repository;

import com.k.medtour.domain.journey.entity.JourneyTemplate;
import com.k.medtour.domain.journey.enums.TemplateCategory;

import java.util.List;
import java.util.Optional;

public interface JourneyTemplateRepository {

    JourneyTemplate save(JourneyTemplate template);

    Optional<JourneyTemplate> findById(Long id);

    void deleteById(Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<JourneyTemplate> findByIdAndDeletedAtIsNull(Long id);

    List<JourneyTemplate> findAllByFilters(String keyword, TemplateCategory category, int page, int size);

    long countByFilters(String keyword, TemplateCategory category);
}
