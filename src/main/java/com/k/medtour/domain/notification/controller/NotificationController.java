package com.k.medtour.domain.notification.controller;

import com.k.medtour.domain.notification.dto.NotificationResponse;
import com.k.medtour.domain.notification.dto.NotificationSendRequest;
import com.k.medtour.domain.notification.dto.UnreadCountResponse;
import com.k.medtour.domain.notification.service.NotificationService;
import com.k.medtour.global.auth.UserPrincipal;
import com.k.medtour.global.common.ApiResponse;
import com.k.medtour.global.common.PageResponse;
import com.k.medtour.server.Router;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    public void register(Router router) {
        router.get("/api/v1/notifications", ctx -> getMyNotifications(
                ctx.userPrincipal(), ctx.queryParamAsInt("page", 0), ctx.queryParamAsInt("size", 20)));
        router.patch("/api/v1/notifications/{id}/read", ctx -> markAsRead(ctx.pathParamAsLong("id"), ctx.userPrincipal()));
        router.post("/api/v1/notifications/read-all", ctx -> markAllAsRead(ctx.userPrincipal()));
        router.get("/api/v1/notifications/unread-count", ctx -> getUnreadCount(ctx.userPrincipal()));
        router.get("/api/v1/admin/alerts", ctx -> getAdminAlerts(
                ctx.queryParamAsInt("page", 0), ctx.queryParamAsInt("size", 20)));
        router.post("/api/v1/admin/notifications", ctx -> sendNotification(ctx.body(NotificationSendRequest.class)));
    }

    /**
     * 내 알림 목록 조회 (페이징)
     */
    public ApiResponse<PageResponse<NotificationResponse>> getMyNotifications(
            UserPrincipal principal,
            int page,
            int size) {
        return ApiResponse.success("조회 성공",
                notificationService.getMyNotifications(principal.memberId(), page, size));
    }

    /**
     * 알림 읽음 처리
     */
    public ApiResponse<NotificationResponse> markAsRead(
            Long id,
            UserPrincipal principal) {
        return ApiResponse.success("읽음 처리 완료",
                notificationService.markAsRead(id, principal.memberId()));
    }

    /**
     * 전체 읽음 처리
     */
    public ApiResponse<Void> markAllAsRead(
            UserPrincipal principal) {
        notificationService.markAllAsRead(principal.memberId());
        return ApiResponse.success("전체 읽음 처리 완료", null);
    }

    /**
     * 미읽은 알림 수
     */
    public ApiResponse<UnreadCountResponse> getUnreadCount(
            UserPrincipal principal) {
        return ApiResponse.success("조회 성공",
                notificationService.getUnreadCount(principal.memberId()));
    }

    /**
     * 긴급 알림 조회 (관리자, SOS/SCHEDULE_CHANGE만)
     */
    public ApiResponse<PageResponse<NotificationResponse>> getAdminAlerts(
            int page,
            int size) {
        return ApiResponse.success("조회 성공",
                notificationService.getAdminAlerts(page, size));
    }

    /**
     * 알림 발송 (관리자, 특정 회원/역할 대상)
     */
    public ApiResponse<List<NotificationResponse>> sendNotification(
            NotificationSendRequest request) {
        return ApiResponse.success("알림 발송 완료",
                notificationService.sendNotification(request));
    }
}
