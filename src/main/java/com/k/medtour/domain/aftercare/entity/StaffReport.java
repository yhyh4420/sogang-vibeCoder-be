package com.k.medtour.domain.aftercare.entity;

import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffReport extends BaseEntity {


    private Long journeyId;


    private Long staffId;


    private String reportContent;


    private Double workHours;


    private LocalDateTime completedAt;

    @Builder
    public StaffReport(Long journeyId, Long staffId, String reportContent,
                       Double workHours) {
        this.journeyId = journeyId;
        this.staffId = staffId;
        this.reportContent = reportContent;
        this.workHours = workHours;
        this.completedAt = LocalDateTime.now();
    }
}
