package com.k.medtour.domain.notification.entity;

import com.k.medtour.domain.notification.enums.NotificationType;
import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {


    private Long memberId;



    private NotificationType type;


    private String title;


    private String content;


    private Long referenceId;


    private String referenceType;


    private Boolean isRead = false;


    private LocalDateTime readAt;

    @Builder
    public Notification(Long memberId, NotificationType type, String title, String content,
                        Long referenceId, String referenceType) {
        this.memberId = memberId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
        this.isRead = false;
    }

    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
        }
    }
}
