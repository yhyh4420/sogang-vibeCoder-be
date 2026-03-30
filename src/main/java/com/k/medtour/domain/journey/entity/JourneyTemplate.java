package com.k.medtour.domain.journey.entity;

import com.k.medtour.domain.journey.enums.TemplateCategory;
import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JourneyTemplate extends BaseEntity {


    private String name;



    private TemplateCategory category;


    private Integer durationDays;


    private Integer usageCount = 0;


    private List<JourneyTemplateItem> items = new ArrayList<>();

    @Builder
    public JourneyTemplate(String name, TemplateCategory category, Integer durationDays) {
        this.name = name;
        this.category = category;
        this.durationDays = durationDays;
        this.usageCount = 0;
    }

    public void update(String name, TemplateCategory category, Integer durationDays) {
        this.name = name;
        this.category = category;
        this.durationDays = durationDays;
    }

    public void incrementUsageCount() {
        this.usageCount++;
    }

    public void replaceItems(List<JourneyTemplateItem> newItems) {
        this.items.clear();
        newItems.forEach(item -> item.assignTemplate(this));
        this.items.addAll(newItems);
    }
}
