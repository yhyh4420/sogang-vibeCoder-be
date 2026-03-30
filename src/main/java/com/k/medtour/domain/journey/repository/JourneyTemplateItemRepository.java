package com.k.medtour.domain.journey.repository;

import com.k.medtour.domain.journey.entity.JourneyTemplateItem;

import java.util.List;

public interface JourneyTemplateItemRepository {

    JourneyTemplateItem save(JourneyTemplateItem item);

    List<JourneyTemplateItem> findByTemplateIdOrderBySortOrderAsc(Long templateId);

    void deleteByTemplateId(Long templateId);
}
