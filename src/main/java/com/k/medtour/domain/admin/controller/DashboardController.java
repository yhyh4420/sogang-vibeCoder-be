package com.k.medtour.domain.admin.controller;

import com.k.medtour.domain.admin.dto.DashboardOverviewResponse;
import com.k.medtour.domain.admin.dto.StaffStatusResponse;
import com.k.medtour.domain.admin.service.DashboardService;
import com.k.medtour.global.common.ApiResponse;
import com.k.medtour.server.Router;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    public void register(Router router) {
        router.get("/api/v1/dashboard/overview", ctx -> getOverview());
        router.get("/api/v1/dashboard/staff-status", ctx -> getStaffStatus());
    }

    /**
     * 운영 현황 요약 (Overview)
     */
    public ApiResponse<DashboardOverviewResponse> getOverview() {
        return ApiResponse.success("조회 성공", dashboardService.getOverview());
    }

    /**
     * 실무자 현황 (가용/업무중/오프라인)
     */
    public ApiResponse<StaffStatusResponse> getStaffStatus() {
        return ApiResponse.success("조회 성공", dashboardService.getStaffStatus());
    }
}
