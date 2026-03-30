package com.k.medtour.domain.journey.entity;

import com.k.medtour.domain.journey.enums.ScheduleItemStatus;
import com.k.medtour.domain.journey.enums.ScheduleItemType;
import com.k.medtour.global.common.BaseEntity;
import com.k.medtour.global.exception.BusinessException;
import com.k.medtour.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JourneyScheduleItem extends BaseEntity {



    private Journey journey;


    private Integer dayNumber;


    private LocalDateTime scheduledAt;


    private String title;



    private ScheduleItemType type;


    private String description;



    private ScheduleItemStatus status = ScheduleItemStatus.SCHEDULED;


    private Integer durationMinutes;



    private Map<String, Object> location;


    private LocalDateTime completedAt;


    private List<StaffAssignment> staffAssignments = new ArrayList<>();

    @Builder
    public JourneyScheduleItem(Journey journey, Integer dayNumber, LocalDateTime scheduledAt,
                                String title, ScheduleItemType type, String description,
                                Integer durationMinutes, Map<String, Object> location) {
        this.journey = journey;
        this.dayNumber = dayNumber;
        this.scheduledAt = scheduledAt;
        this.title = title;
        this.type = type;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.location = location;
        this.status = ScheduleItemStatus.SCHEDULED;
    }

    public void updateStatus(ScheduleItemStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "상태 전이 불가: " + this.status + " -> " + newStatus);
        }
        this.status = newStatus;
        if (newStatus == ScheduleItemStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public void update(LocalDateTime scheduledAt, String title, String description,
                       Integer durationMinutes, Map<String, Object> location) {
        if (this.status == ScheduleItemStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.SCHEDULE_ITEM_COMPLETED,
                    "이미 완료된 일정은 수정할 수 없습니다.");
        }
        if (scheduledAt != null) this.scheduledAt = scheduledAt;
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (durationMinutes != null) this.durationMinutes = durationMinutes;
        if (location != null) this.location = location;
    }

    public boolean isModifiable() {
        return this.status != ScheduleItemStatus.COMPLETED
                && this.status != ScheduleItemStatus.IN_PROGRESS
                && this.status != ScheduleItemStatus.CANCELLED;
    }
}
