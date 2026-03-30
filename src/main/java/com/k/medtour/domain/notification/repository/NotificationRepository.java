package com.k.medtour.domain.notification.repository;

import com.k.medtour.domain.notification.entity.Notification;
import com.k.medtour.domain.notification.enums.NotificationType;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    List<Notification> findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long memberId, int page, int size);

    long countByMemberIdAndDeletedAtIsNull(Long memberId);

    long countByMemberIdAndIsReadFalseAndDeletedAtIsNull(Long memberId);

    int markAllAsReadByMemberId(Long memberId);

    List<Notification> findByTypeInOrderByCreatedAtDesc(List<NotificationType> types, int page, int size);

    long countByTypeIn(List<NotificationType> types);
}
