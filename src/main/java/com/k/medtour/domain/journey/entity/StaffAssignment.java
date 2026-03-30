package com.k.medtour.domain.journey.entity;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.journey.enums.StaffAssignmentStatus;
import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffAssignment extends BaseEntity {



    private JourneyScheduleItem scheduleItem;



    private Member staff;



    private StaffAssignmentStatus status = StaffAssignmentStatus.ASSIGNED;


    private LocalDateTime assignedAt;


    private LocalDateTime completedAt;

    @Builder
    public StaffAssignment(JourneyScheduleItem scheduleItem, Member staff) {
        this.scheduleItem = scheduleItem;
        this.staff = staff;
        this.status = StaffAssignmentStatus.ASSIGNED;
        this.assignedAt = LocalDateTime.now();
    }

    public void startDuty() {
        this.status = StaffAssignmentStatus.ON_DUTY;
    }

    public void complete() {
        this.status = StaffAssignmentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
