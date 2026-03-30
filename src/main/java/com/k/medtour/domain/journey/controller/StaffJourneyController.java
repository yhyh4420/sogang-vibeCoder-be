package com.k.medtour.domain.journey.controller;

import com.k.medtour.domain.journey.dto.NavigationResponse;
import com.k.medtour.domain.journey.dto.PatientNoticeResponse;
import com.k.medtour.domain.journey.dto.StaffTodayResponse;
import com.k.medtour.domain.journey.dto.StatusUpdateRequest;
import com.k.medtour.domain.journey.dto.StatusUpdateResponse;
import com.k.medtour.domain.journey.service.StaffAssignmentService;
import com.k.medtour.global.auth.UserPrincipal;
import com.k.medtour.global.common.ApiResponse;
import com.k.medtour.server.Router;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@RequiredArgsConstructor
public class StaffJourneyController {

    private final StaffAssignmentService staffAssignmentService;

    public void register(Router router) {
        router.get("/api/v1/staff/today", ctx -> {
            String dateStr = ctx.queryParam("date");
            LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : null;
            return getTodayTasks(ctx.userPrincipal(), date);
        });
        router.get("/api/v1/staff/journeys/{journeyId}/notice", ctx -> getPatientNotice(
                ctx.pathParamAsLong("journeyId"), ctx.userPrincipal()));
        router.post("/api/v1/staff/journeys/{journeyId}/items/{itemId}/status", ctx -> updateStatus(
                ctx.pathParamAsLong("journeyId"), ctx.pathParamAsLong("itemId"),
                ctx.userPrincipal(), ctx.body(StatusUpdateRequest.class)));
        router.get("/api/v1/staff/journeys/{journeyId}/items/{itemId}/navigation", ctx -> getNavigation(
                ctx.pathParamAsLong("journeyId"), ctx.pathParamAsLong("itemId"),
                ctx.queryParam("platform", "google")));
    }

    public ApiResponse<StaffTodayResponse> getTodayTasks(
            UserPrincipal principal,
            LocalDate date) {
        return ApiResponse.success("조회 성공",
                staffAssignmentService.getTodayTasks(principal.memberId(), date));
    }

    public ApiResponse<PatientNoticeResponse> getPatientNotice(
            Long journeyId,
            UserPrincipal principal) {
        return ApiResponse.success("조회 성공",
                staffAssignmentService.getPatientNotice(journeyId, principal.memberId()));
    }

    public ApiResponse<StatusUpdateResponse> updateStatus(
            Long journeyId,
            Long itemId,
            UserPrincipal principal,
            StatusUpdateRequest request) {
        return ApiResponse.success("상태 업데이트 완료",
                staffAssignmentService.updateStatus(journeyId, itemId, principal.memberId(), request));
    }

    public ApiResponse<NavigationResponse> getNavigation(
            Long journeyId,
            Long itemId,
            String platform) {
        return ApiResponse.success("조회 성공",
                staffAssignmentService.getNavigation(journeyId, itemId, platform));
    }
}
