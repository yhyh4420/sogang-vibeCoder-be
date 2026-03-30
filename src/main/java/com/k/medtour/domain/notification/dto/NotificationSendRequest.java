package com.k.medtour.domain.notification.dto;

import com.k.medtour.domain.notification.enums.NotificationType;

import java.util.List;

public record NotificationSendRequest(
        NotificationType type,
        String title,
        String content,
        Long referenceId,
        String referenceType,
        List<Long> memberIds,
        String targetRole
) {
}
