package com.k.medtour.domain.admin.controller;

import com.k.medtour.domain.admin.dto.*;
import com.k.medtour.domain.admin.service.AuthService;
import com.k.medtour.global.auth.UserPrincipal;
import com.k.medtour.global.common.ApiResponse;
import com.k.medtour.server.Router;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    public void register(Router router) {
        router.post("/api/v1/auth/oauth/{provider}", ctx -> {
            String provider = ctx.pathParam("provider");
            var request = ctx.body(OAuthLoginRequest.class);
            return oauthLogin(provider, request);
        });
        router.post("/api/v1/auth/magic-link", ctx -> createMagicLink(ctx.body(MagicLinkRequest.class)));
        router.post("/api/v1/auth/magic-link/verify", ctx -> verifyMagicLink(ctx.body(MagicLinkVerifyRequest.class)));
        router.post("/api/v1/auth/refresh", ctx -> {
            var body = ctx.body(java.util.Map.class);
            return refresh((String) body.get("refreshToken"));
        });
        router.post("/api/v1/auth/logout", ctx -> logout(ctx.userPrincipal()));
        router.post("/api/v1/auth/consent", ctx -> submitConsent(ctx.userPrincipal(), ctx.body(ConsentRequest.class)));
        router.get("/api/v1/auth/consent", ctx -> getConsent(ctx.userPrincipal()));
        router.get("/api/v1/auth/roles", ctx -> getRoles());
        router.put("/api/v1/auth/users/{userId}/role", ctx -> changeUserRole(
                ctx.userPrincipal(), ctx.pathParamAsLong("userId"), ctx.body(RoleChangeRequest.class)));
    }

    /**
     * OAuth2 소셜 로그인
     * POST /api/v1/auth/oauth/{provider}
     */
    public ApiResponse<OAuthLoginResponse> oauthLogin(
            String provider,
            OAuthLoginRequest request) {

        OAuthLoginResponse loginResponse = authService.oauthLogin(provider, request);
        return ApiResponse.success("로그인 성공", loginResponse);
    }

    /**
     * 매직 링크 발급
     * POST /api/v1/auth/magic-link
     */
    public ApiResponse<MagicLinkResponse> createMagicLink(MagicLinkRequest request) {
        MagicLinkResponse magicLinkResponse = authService.createMagicLink(request);
        return ApiResponse.success("매직 링크가 전송되었습니다", magicLinkResponse);
    }

    /**
     * 매직 링크 인증 + 2FA
     * POST /api/v1/auth/magic-link/verify
     */
    public ApiResponse<MagicLinkVerifyResponse> verifyMagicLink(MagicLinkVerifyRequest request) {
        MagicLinkVerifyResponse verifyResponse = authService.verifyMagicLink(request);
        return ApiResponse.success("인증 성공", verifyResponse);
    }

    /**
     * 토큰 갱신
     * POST /api/v1/auth/refresh
     */
    public ApiResponse<TokenResponse> refresh(String refreshToken) {
        TokenResponse tokenResponse = authService.refreshToken(refreshToken);
        return ApiResponse.success("토큰 갱신 성공", tokenResponse);
    }

    /**
     * 로그아웃
     * POST /api/v1/auth/logout
     */
    public ApiResponse<Void> logout(UserPrincipal principal) {
        authService.logout(principal.memberId());
        return ApiResponse.success("로그아웃 성공", null);
    }

    /**
     * 약관 동의
     * POST /api/v1/auth/consent
     */
    public ApiResponse<ConsentResponse> submitConsent(
            UserPrincipal principal,
            ConsentRequest request) {

        ConsentResponse consentResponse = authService.submitConsent(principal.memberId(), request);
        return ApiResponse.success("동의 처리 완료", consentResponse);
    }

    /**
     * 약관 동의 내역 조회
     * GET /api/v1/auth/consent
     */
    public ApiResponse<ConsentResponse> getConsent(UserPrincipal principal) {
        ConsentResponse consentResponse = authService.getConsent(principal.memberId());
        return ApiResponse.success("조회 성공", consentResponse);
    }

    /**
     * 역할 목록 조회
     * GET /api/v1/auth/roles
     */
    public ApiResponse<List<RoleResponse>> getRoles() {
        List<RoleResponse> roles = authService.getRoles();
        return ApiResponse.success("조회 성공", roles);
    }

    /**
     * 사용자 역할 변경
     * PUT /api/v1/auth/users/{userId}/role
     */
    public ApiResponse<RoleChangeResponse> changeUserRole(
            UserPrincipal principal,
            Long userId,
            RoleChangeRequest request) {

        RoleChangeResponse roleChangeResponse = authService.changeUserRole(principal.memberId(), userId, request);
        return ApiResponse.success("역할 변경 완료", roleChangeResponse);
    }
}
