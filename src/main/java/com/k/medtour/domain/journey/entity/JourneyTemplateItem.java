package com.k.medtour.domain.journey.entity;

import com.k.medtour.domain.journey.enums.ScheduleItemType;
import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JourneyTemplateItem extends BaseEntity {



    private JourneyTemplate template;


    private Integer dayOffset;


    private String timeOffset;


    private String title;



    private ScheduleItemType type;


    private String description;


    private Integer durationMinutes;



    private Map<String, Object> location;



    private List<String> requiredStaff;


    private Integer sortOrder;

    @Builder
    public JourneyTemplateItem(Integer dayOffset, String timeOffset, String title,
                                ScheduleItemType type, String description, Integer durationMinutes,
                                Map<String, Object> location, List<String> requiredStaff, Integer sortOrder) {
        this.dayOffset = dayOffset;
        this.timeOffset = timeOffset;
        this.title = title;
        this.type = type;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.location = location;
        this.requiredStaff = requiredStaff;
        this.sortOrder = sortOrder;
    }

    public void assignTemplate(JourneyTemplate template) {
        this.template = template;
    }
}
