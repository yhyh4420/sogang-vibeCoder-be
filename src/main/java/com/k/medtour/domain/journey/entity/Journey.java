package com.k.medtour.domain.journey.entity;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.journey.enums.JourneyStatus;
import com.k.medtour.global.common.BaseEntity;
import com.k.medtour.global.exception.BusinessException;
import com.k.medtour.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Journey extends BaseEntity {



    private Member patient;


    private String title;



    private JourneyStatus status = JourneyStatus.PLANNED;


    private LocalDate startDate;


    private LocalDate endDate;


    private String notes;


    private List<JourneyScheduleItem> scheduleItems = new ArrayList<>();

    @Builder
    public Journey(Member patient, String title, LocalDate startDate, LocalDate endDate, String notes) {
        this.patient = patient;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.notes = notes;
        this.status = JourneyStatus.PLANNED;
    }

    public void start() {
        if (this.status != JourneyStatus.PLANNED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "여정을 시작할 수 없는 상태입니다: " + this.status);
        }
        this.status = JourneyStatus.IN_PROGRESS;
    }

    public void complete() {
        if (this.status != JourneyStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "여정을 완료할 수 없는 상태입니다: " + this.status);
        }
        this.status = JourneyStatus.COMPLETED;
    }

    public void cancel() {
        if (this.status == JourneyStatus.COMPLETED || this.status == JourneyStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "여정을 취소할 수 없는 상태입니다: " + this.status);
        }
        this.status = JourneyStatus.CANCELLED;
    }

    public void addScheduleItem(JourneyScheduleItem item) {
        this.scheduleItems.add(item);
    }
}
