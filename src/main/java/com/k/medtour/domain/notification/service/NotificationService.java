package com.k.medtour.domain.notification.service;

import com.k.medtour.domain.admin.repository.MemberRepository;
import com.k.medtour.domain.notification.dto.NotificationResponse;
import com.k.medtour.domain.notification.dto.NotificationSendRequest;
import com.k.medtour.domain.notification.dto.UnreadCountResponse;
import com.k.medtour.domain.notification.entity.Notification;
import com.k.medtour.domain.notification.enums.NotificationType;
import com.k.medtour.domain.notification.repository.NotificationRepository;
import com.k.medtour.global.common.PageResponse;
import com.k.medtour.global.exception.BusinessException;
import com.k.medtour.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

    public PageResponse<NotificationResponse> getMyNotifications(Long memberId, int page, int size) {
        List<Notification> notifications = notificationRepository
                .findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId, page, size);
        long totalElements = notificationRepository.countByMemberIdAndDeletedAtIsNull(memberId);
        List<NotificationResponse> content = notifications.stream().map(NotificationResponse::from).toList();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    public NotificationResponse markAsRead(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인의 알림만 읽음 처리할 수 있습니다.");
        }

        notification.markAsRead();
        return NotificationResponse.from(notification);
    }

    public void markAllAsRead(Long memberId) {
        notificationRepository.markAllAsReadByMemberId(memberId);
    }

    public UnreadCountResponse getUnreadCount(Long memberId) {
        long count = notificationRepository.countByMemberIdAndIsReadFalseAndDeletedAtIsNull(memberId);
        return new UnreadCountResponse(count);
    }

    public PageResponse<NotificationResponse> getAdminAlerts(int page, int size) {
        List<NotificationType> alertTypes = List.of(NotificationType.SOS, NotificationType.SCHEDULE_CHANGE);
        List<Notification> notifications = notificationRepository
                .findByTypeInOrderByCreatedAtDesc(alertTypes, page, size);
        long totalElements = notificationRepository.countByTypeIn(alertTypes);
        List<NotificationResponse> content = notifications.stream().map(NotificationResponse::from).toList();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    public List<NotificationResponse> sendNotification(NotificationSendRequest request) {
        List<Long> targetMemberIds = new ArrayList<>();

        if (request.memberIds() != null && !request.memberIds().isEmpty()) {
            targetMemberIds.addAll(request.memberIds());
        }

        if (request.targetRole() != null && !request.targetRole().isBlank()) {
            memberRepository.findAllByRoleName(request.targetRole(), 0, 1000)
                    .forEach(member -> targetMemberIds.add(member.getId()));
        }

        List<NotificationResponse> results = new ArrayList<>();
        for (Long memberId : targetMemberIds.stream().distinct().toList()) {
            Notification notification = Notification.builder()
                    .memberId(memberId)
                    .type(request.type())
                    .title(request.title())
                    .content(request.content())
                    .referenceId(request.referenceId())
                    .referenceType(request.referenceType())
                    .build();
            Notification saved = notificationRepository.save(notification);
            results.add(NotificationResponse.from(saved));
        }
        return results;
    }

    public Notification createNotification(Long memberId, NotificationType type,
                                           String title, String content,
                                           Long referenceId, String referenceType) {
        Notification notification = Notification.builder()
                .memberId(memberId)
                .type(type)
                .title(title)
                .content(content)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();
        return notificationRepository.save(notification);
    }
}
