package com.k.medtour.domain.aftercare.controller;

import com.k.medtour.domain.aftercare.dto.AftercareGuideCreateRequest;
import com.k.medtour.domain.aftercare.dto.AftercareGuideResponse;
import com.k.medtour.domain.aftercare.dto.InvoiceCreateRequest;
import com.k.medtour.domain.aftercare.dto.InvoiceResponse;
import com.k.medtour.domain.aftercare.dto.StaffReportCreateRequest;
import com.k.medtour.domain.aftercare.dto.StaffReportResponse;
import com.k.medtour.domain.aftercare.service.AftercareService;
import com.k.medtour.global.auth.UserPrincipal;
import com.k.medtour.global.common.ApiResponse;
import com.k.medtour.server.Router;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class AftercareController {

    private final AftercareService aftercareService;

    public void register(Router router) {
        router.post("/api/v1/aftercare/guides", ctx -> createGuide(ctx.body(AftercareGuideCreateRequest.class)));
        router.get("/api/v1/aftercare/guides/{journeyId}", ctx -> getGuide(ctx.pathParamAsLong("journeyId")));
        router.post("/api/v1/aftercare/invoices", ctx -> createInvoice(ctx.body(InvoiceCreateRequest.class)));
        router.get("/api/v1/aftercare/invoices/{journeyId}", ctx -> getInvoice(ctx.pathParamAsLong("journeyId")));
        router.get("/api/v1/aftercare/invoices/me", ctx -> getMyInvoices(ctx.userPrincipal()));
        router.post("/api/v1/aftercare/reports", ctx -> createReport(ctx.body(StaffReportCreateRequest.class), ctx.userPrincipal()));
    }

    /**
     * 사후 관리 가이드 생성 (ADMIN)
     */
    public ApiResponse<AftercareGuideResponse> createGuide(
            AftercareGuideCreateRequest request) {
        return ApiResponse.success("가이드 생성 완료", aftercareService.createGuide(request));
    }

    /**
     * 사후 관리 가이드 조회
     */
    public ApiResponse<AftercareGuideResponse> getGuide(Long journeyId) {
        return ApiResponse.success("조회 성공", aftercareService.getGuide(journeyId));
    }

    /**
     * 인보이스 생성 (ADMIN)
     */
    public ApiResponse<InvoiceResponse> createInvoice(
            InvoiceCreateRequest request) {
        return ApiResponse.success("인보이스 생성 완료", aftercareService.createInvoice(request));
    }

    /**
     * 인보이스 조회 (여정별)
     */
    public ApiResponse<InvoiceResponse> getInvoice(Long journeyId) {
        return ApiResponse.success("조회 성공", aftercareService.getInvoiceByJourneyId(journeyId));
    }

    /**
     * 내 인보이스 목록 (PATIENT)
     */
    public ApiResponse<List<InvoiceResponse>> getMyInvoices(
            UserPrincipal principal) {
        return ApiResponse.success("조회 성공", aftercareService.getMyInvoices(principal.memberId()));
    }

    /**
     * 업무 종료 리포트 (STAFF)
     */
    public ApiResponse<StaffReportResponse> createReport(
            StaffReportCreateRequest request,
            UserPrincipal principal) {
        return ApiResponse.success("리포트 생성 완료",
                aftercareService.createReport(request, principal.memberId()));
    }
}
