package com.k.medtour.domain.admin.controller;

import com.k.medtour.domain.admin.dto.*;
import com.k.medtour.domain.admin.service.MemberService;
import com.k.medtour.global.auth.UserPrincipal;
import com.k.medtour.global.common.ApiResponse;
import com.k.medtour.server.Router;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    public void register(Router router) {
        router.get("/api/v1/members/staff/me", ctx -> getStaffProfile(ctx.userPrincipal()));
        router.put("/api/v1/members/staff/me", ctx -> updateStaffProfile(ctx.userPrincipal(), ctx.body(StaffProfileUpdateRequest.class)));
        router.get("/api/v1/members/agency", ctx -> getAgencyProfile());
        router.put("/api/v1/members/agency", ctx -> updateAgencyProfile(ctx.body(AgencyProfileUpdateRequest.class)));
        router.get("/api/v1/members/agency/license", ctx -> verifyLicense());
    }

    /**
     * 실무자 본인 프로필 조회
     * GET /api/v1/members/staff/me
     */
    public ApiResponse<StaffProfileResponse> getStaffProfile(UserPrincipal principal) {
        StaffProfileResponse staffProfile = memberService.getStaffProfile(principal.memberId());
        return ApiResponse.success("조회 성공", staffProfile);
    }

    /**
     * 실무자 본인 프로필 수정
     * PUT /api/v1/members/staff/me
     */
    public ApiResponse<StaffProfileResponse> updateStaffProfile(
            UserPrincipal principal,
            StaffProfileUpdateRequest request) {

        StaffProfileResponse staffProfile = memberService.updateStaffProfile(principal.memberId(), request);
        return ApiResponse.success("프로필 수정 완료", staffProfile);
    }

    /**
     * 에이전시 프로필 조회
     * GET /api/v1/members/agency
     */
    public ApiResponse<AgencyProfileResponse> getAgencyProfile() {
        AgencyProfileResponse agencyProfile = memberService.getAgencyProfile();
        return ApiResponse.success("조회 성공", agencyProfile);
    }

    /**
     * 에이전시 프로필 수정
     * PUT /api/v1/members/agency
     */
    public ApiResponse<AgencyProfileResponse> updateAgencyProfile(AgencyProfileUpdateRequest request) {
        AgencyProfileResponse agencyProfile = memberService.updateAgencyProfile(request);
        return ApiResponse.success("수정 완료", agencyProfile);
    }

    /**
     * 에이전시 라이선스 검증
     * GET /api/v1/members/agency/license
     */
    public ApiResponse<LicenseVerifyResponse> verifyLicense() {
        LicenseVerifyResponse licenseVerify = memberService.verifyLicense();
        return ApiResponse.success("조회 성공", licenseVerify);
    }
}
