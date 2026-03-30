package com.k.medtour.domain.journey.controller;

import com.k.medtour.domain.journey.dto.JourneyCreateRequest;
import com.k.medtour.domain.journey.dto.JourneyDetailResponse;
import com.k.medtour.domain.journey.dto.JourneyListResponse;
import com.k.medtour.domain.journey.dto.JourneyResponse;
import com.k.medtour.domain.journey.dto.ScheduleItemCreateRequest;
import com.k.medtour.domain.journey.dto.ScheduleItemResponse;
import com.k.medtour.domain.journey.dto.ScheduleItemUpdateRequest;
import com.k.medtour.domain.journey.dto.StaffAssignRequest;
import com.k.medtour.domain.journey.dto.TemplateCreateRequest;
import com.k.medtour.domain.journey.dto.TemplateListResponse;
import com.k.medtour.domain.journey.dto.TemplateResponse;
import com.k.medtour.domain.journey.dto.TimelineResponse;
import com.k.medtour.domain.journey.enums.JourneyStatus;
import com.k.medtour.domain.journey.enums.TemplateCategory;
import com.k.medtour.domain.journey.service.JourneyService;
import com.k.medtour.global.auth.UserPrincipal;
import com.k.medtour.global.common.ApiResponse;
import com.k.medtour.global.common.PageResponse;
import com.k.medtour.server.Router;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@RequiredArgsConstructor
public class JourneyController {

    private final JourneyService journeyService;

    public void register(Router router) {
        // Templates
        router.get("/api/v1/journey-templates", ctx -> {
            String catStr = ctx.queryParam("category");
            TemplateCategory cat = catStr != null ? TemplateCategory.valueOf(catStr) : null;
            return getTemplates(ctx.queryParam("keyword"), cat, ctx.queryParamAsInt("page", 0), ctx.queryParamAsInt("size", 20));
        });
        router.post("/api/v1/journey-templates", ctx -> createTemplate(ctx.body(TemplateCreateRequest.class)));
        router.get("/api/v1/journey-templates/{templateId}", ctx -> getTemplate(ctx.pathParamAsLong("templateId")));
        router.put("/api/v1/journey-templates/{templateId}", ctx -> updateTemplate(ctx.pathParamAsLong("templateId"), ctx.body(TemplateCreateRequest.class)));
        router.delete("/api/v1/journey-templates/{templateId}", ctx -> deleteTemplate(ctx.pathParamAsLong("templateId")));
        // Journeys
        router.post("/api/v1/journeys", ctx -> createJourney(ctx.body(JourneyCreateRequest.class)));
        router.get("/api/v1/journeys", ctx -> {
            String statusStr = ctx.queryParam("status");
            JourneyStatus status = statusStr != null ? JourneyStatus.valueOf(statusStr) : null;
            String fromStr = ctx.queryParam("startDateFrom");
            String toStr = ctx.queryParam("startDateTo");
            LocalDate from = fromStr != null ? LocalDate.parse(fromStr) : null;
            LocalDate to = toStr != null ? LocalDate.parse(toStr) : null;
            return getJourneys(status, ctx.queryParamAsLong("patientId"), from, to,
                    ctx.queryParamAsInt("page", 0), ctx.queryParamAsInt("size", 20));
        });
        router.get("/api/v1/journeys/{journeyId}", ctx -> getJourneyDetail(ctx.pathParamAsLong("journeyId"), ctx.userPrincipal()));
        router.post("/api/v1/journeys/{journeyId}/assign", ctx -> assignStaff(ctx.pathParamAsLong("journeyId"), ctx.body(StaffAssignRequest.class)));
        // Schedule Items
        router.post("/api/v1/journeys/{journeyId}/items", ctx -> addScheduleItem(ctx.pathParamAsLong("journeyId"), ctx.body(ScheduleItemCreateRequest.class)));
        router.put("/api/v1/journeys/{journeyId}/items/{itemId}", ctx -> updateScheduleItem(
                ctx.pathParamAsLong("journeyId"), ctx.pathParamAsLong("itemId"), ctx.body(ScheduleItemUpdateRequest.class)));
        router.delete("/api/v1/journeys/{journeyId}/items/{itemId}", ctx -> deleteScheduleItem(
                ctx.pathParamAsLong("journeyId"), ctx.pathParamAsLong("itemId")));
        // Patient Timeline
        router.get("/api/v1/timeline", ctx -> {
            String dateStr = ctx.queryParam("date");
            LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : null;
            return getTimeline(ctx.userPrincipal(), date);
        });
    }

    // ======================== Template Endpoints (ADMIN) ========================

    public ApiResponse<PageResponse<TemplateListResponse>> getTemplates(
            String keyword,
            TemplateCategory category,
            int page,
            int size) {
        return ApiResponse.success("조회 성공", journeyService.getTemplates(keyword, category, page, size));
    }

    public ApiResponse<TemplateResponse> createTemplate(
            TemplateCreateRequest request) {
        TemplateResponse response = journeyService.createTemplate(request);
        return ApiResponse.success("템플릿 생성 완료", response);
    }

    public ApiResponse<TemplateResponse> getTemplate(Long templateId) {
        return ApiResponse.success("조회 성공", journeyService.getTemplate(templateId));
    }

    public ApiResponse<TemplateResponse> updateTemplate(
            Long templateId,
            TemplateCreateRequest request) {
        return ApiResponse.success("템플릿 수정 완료", journeyService.updateTemplate(templateId, request));
    }

    public ApiResponse<Void> deleteTemplate(Long templateId) {
        journeyService.deleteTemplate(templateId);
        return ApiResponse.success("템플릿 삭제 완료", null);
    }

    // ======================== Journey Endpoints ========================

    public ApiResponse<JourneyResponse> createJourney(
            JourneyCreateRequest request) {
        JourneyResponse response = journeyService.createJourney(request);
        return ApiResponse.success("여정 생성 완료", response);
    }

    public ApiResponse<PageResponse<JourneyListResponse>> getJourneys(
            JourneyStatus status,
            Long patientId,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            int page,
            int size) {
        return ApiResponse.success("조회 성공",
                journeyService.getJourneys(status, patientId, startDateFrom, startDateTo, page, size));
    }

    public ApiResponse<JourneyDetailResponse> getJourneyDetail(
            Long journeyId,
            UserPrincipal principal) {
        return ApiResponse.success("조회 성공", journeyService.getJourneyDetail(journeyId, principal));
    }

    public ApiResponse<Void> assignStaff(
            Long journeyId,
            StaffAssignRequest request) {
        journeyService.assignStaff(journeyId, request);
        return ApiResponse.success("실무자 배정 완료", null);
    }

    // ======================== Schedule Item Endpoints (ADMIN) ========================

    public ApiResponse<ScheduleItemResponse> addScheduleItem(
            Long journeyId,
            ScheduleItemCreateRequest request) {
        ScheduleItemResponse response = journeyService.addScheduleItem(journeyId, request);
        return ApiResponse.success("일정 추가 완료", response);
    }

    public ApiResponse<ScheduleItemResponse> updateScheduleItem(
            Long journeyId,
            Long itemId,
            ScheduleItemUpdateRequest request) {
        return ApiResponse.success("일정 수정 완료",
                journeyService.updateScheduleItem(journeyId, itemId, request));
    }

    public ApiResponse<Void> deleteScheduleItem(
            Long journeyId,
            Long itemId) {
        journeyService.deleteScheduleItem(journeyId, itemId);
        return ApiResponse.success("일정 삭제 완료", null);
    }

    // ======================== Patient Timeline ========================

    public ApiResponse<TimelineResponse> getTimeline(
            UserPrincipal principal,
            LocalDate date) {
        return ApiResponse.success("조회 성공", journeyService.getTimeline(principal.memberId(), date));
    }
}
