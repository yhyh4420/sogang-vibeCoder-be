package com.k.medtour.domain.profile.controller;

import com.k.medtour.domain.profile.dto.PortfolioResponse;
import com.k.medtour.domain.profile.service.ProfileService;
import com.k.medtour.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * 포트폴리오 조회 (Before & After)
     */
    public ApiResponse<PortfolioResponse> getPortfolio(Long id) {
        return ApiResponse.success("조회 성공", profileService.getPortfolio(id));
    }
}
