package com.k.medtour.domain.profile.controller;

import com.k.medtour.domain.profile.dto.PortfolioResponse;
import com.k.medtour.domain.profile.service.ProfileService;
import com.k.medtour.global.common.ApiResponse;
import com.k.medtour.server.Router;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    public void register(Router router) {
        router.get("/api/v1/profiles/{id}/portfolio", ctx -> getPortfolio(ctx.pathParamAsLong("id")));
    }

    /**
     * 포트폴리오 조회 (Before & After)
     */
    public ApiResponse<PortfolioResponse> getPortfolio(Long id) {
        return ApiResponse.success("조회 성공", profileService.getPortfolio(id));
    }
}
