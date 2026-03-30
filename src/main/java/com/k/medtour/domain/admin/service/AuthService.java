package com.k.medtour.domain.admin.service;

import com.k.medtour.domain.admin.dto.*;
import com.k.medtour.domain.admin.entity.MagicLink;
import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.admin.entity.MemberConsent;
import com.k.medtour.domain.admin.entity.RefreshToken;
import com.k.medtour.domain.admin.entity.Role;
import com.k.medtour.domain.admin.enums.MagicLinkTargetType;
import com.k.medtour.domain.admin.repository.*;
import com.k.medtour.global.auth.jwt.JwtProperties;
import com.k.medtour.global.auth.jwt.JwtTokenProvider;
import com.k.medtour.global.exception.BusinessException;
import com.k.medtour.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private static final int MAGIC_LINK_EXPIRY_MINUTES = 10;
    private static final int MAGIC_LINK_RATE_LIMIT = 3;

    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final MagicLinkRepository magicLinkRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberConsentRepository memberConsentRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    /**
     * OAuth2 소셜 로그인 처리
     */
    public OAuthLoginResponse oauthLogin(String provider, OAuthLoginRequest request) {
        log.info("OAuth 로그인 시도: provider={}", provider);

        validateOAuthProvider(provider);

        // TODO: 실제 Google/Apple idToken 검증 로직 구현 필요
        // 현재 MVP에서는 idToken에서 이메일과 이름을 간소화하여 추출
        String email = extractEmailFromIdToken(request.idToken());
        String name = extractNameFromIdToken(request.idToken());

        // 이미 다른 제공자로 가입된 이메일 확인
        memberRepository.findByEmail(email).ifPresent(existing -> {
            if (existing.getOauthProvider() != null && !existing.getOauthProvider().equals(provider)) {
                throw new BusinessException(ErrorCode.OAUTH_EMAIL_CONFLICT);
            }
        });

        boolean isNewUser = false;
        Member member = memberRepository.findByOauthProviderAndOauthId(provider, request.idToken())
                .orElse(null);

        if (member == null) {
            member = memberRepository.findByEmail(email).orElse(null);
        }

        if (member == null) {
            isNewUser = true;
            Role patientRole = roleRepository.findByName("ROLE_PATIENT")
                    .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

            String language = request.deviceInfo() != null && request.deviceInfo().language() != null
                    ? request.deviceInfo().language() : "en";

            member = Member.builder()
                    .email(email)
                    .name(name)
                    .role(patientRole)
                    .oauthProvider(provider)
                    .oauthId(request.idToken())
                    .language(language)
                    .build();
            member = memberRepository.save(member);
            log.info("새 회원 생성: memberId={}, email={}", member.getId(), email);
        }

        List<String> permissions = member.getRole().getPermissions().stream()
                .map(p -> p.getName())
                .toList();

        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().getName(), permissions);
        String refreshTokenStr = jwtTokenProvider.createRefreshToken(member.getId());

        saveRefreshToken(member, refreshTokenStr);

        UserInfo userInfo = UserInfo.from(member, isNewUser);
        long expiresIn = jwtProperties.accessTokenExpiration() / 1000;

        log.info("OAuth 로그인 성공: memberId={}, isNewUser={}", member.getId(), isNewUser);
        return OAuthLoginResponse.of(accessToken, expiresIn, userInfo);
    }

    /**
     * 매직 링크 발급
     */
    public MagicLinkResponse createMagicLink(MagicLinkRequest request) {
        log.info("매직 링크 발급 요청: target={}, targetType={}", maskTarget(request.target()), request.targetType());

        MagicLinkTargetType targetType = MagicLinkTargetType.valueOf(request.targetType());

        // 분당 3회 제한 체크
        checkRateLimit(request.target(), targetType);

        MagicLink magicLink = MagicLink.builder()
                .token(UUID.randomUUID())
                .targetEmail(targetType == MagicLinkTargetType.EMAIL ? request.target() : null)
                .targetPhone(targetType == MagicLinkTargetType.SMS ? request.target() : null)
                .targetType(targetType)
                .role(request.role())
                .language(request.language() != null ? request.language() : "en")
                .expiresAt(LocalDateTime.now().plusMinutes(MAGIC_LINK_EXPIRY_MINUTES))
                .build();

        magicLinkRepository.save(magicLink);

        // TODO: 실제 이메일/SMS 발송 로직 구현 필요
        log.info("매직 링크 생성 완료: token={}", magicLink.getToken());

        return new MagicLinkResponse(
                MAGIC_LINK_EXPIRY_MINUTES * 60L,
                maskTarget(request.target())
        );
    }

    /**
     * 매직 링크 인증 + 2FA (생년월일)
     */
    public MagicLinkVerifyResponse verifyMagicLink(MagicLinkVerifyRequest request) {
        log.info("매직 링크 인증 시도");

        UUID tokenUuid = UUID.fromString(request.token());
        MagicLink magicLink = magicLinkRepository.findByToken(tokenUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.MAGIC_LINK_NOT_FOUND));

        if (magicLink.isExpired()) {
            throw new BusinessException(ErrorCode.MAGIC_LINK_EXPIRED);
        }

        if (magicLink.isUsed()) {
            throw new BusinessException(ErrorCode.MAGIC_LINK_EXPIRED, "이미 사용된 매직 링크입니다.");
        }

        // 2FA: 생년월일 검증
        if (magicLink.getBirthDate() != null && !magicLink.getBirthDate().equals(request.birthDate())) {
            throw new BusinessException(ErrorCode.MAGIC_LINK_BIRTH_DATE_MISMATCH);
        }

        magicLink.markAsUsed();

        // 회원 조회 또는 생성
        String target = magicLink.getTargetEmail() != null ? magicLink.getTargetEmail() : magicLink.getTargetPhone();
        Member member = memberRepository.findByEmail(target).orElseGet(() -> {
            Role role = roleRepository.findByName(magicLink.getRole())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));
            Member newMember = Member.builder()
                    .email(target)
                    .name("Guest")
                    .role(role)
                    .language(magicLink.getLanguage())
                    .build();
            return memberRepository.save(newMember);
        });

        magicLink.linkMember(member);

        List<String> permissions = member.getRole().getPermissions().stream()
                .map(p -> p.getName())
                .toList();

        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().getName(), permissions);
        long expiresIn = jwtProperties.accessTokenExpiration() / 1000;

        UserInfo userInfo = UserInfo.from(member, false);
        log.info("매직 링크 인증 성공: memberId={}", member.getId());

        return MagicLinkVerifyResponse.of(accessToken, expiresIn, userInfo);
    }

    /**
     * Refresh Token으로 Access Token 갱신
     */
    public TokenResponse refreshToken(String refreshTokenStr) {
        log.info("토큰 갱신 요청");

        if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (!refreshToken.isValid()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        Member member = memberRepository.findByIdWithRole(refreshToken.getMember().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<String> permissions = member.getRole().getPermissions().stream()
                .map(p -> p.getName())
                .toList();

        String newAccessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole().getName(), permissions);
        long expiresIn = jwtProperties.accessTokenExpiration() / 1000;

        log.info("토큰 갱신 성공: memberId={}", member.getId());
        return TokenResponse.of(newAccessToken, expiresIn);
    }

    /**
     * 로그아웃 (Refresh Token 무효화)
     */
    public void logout(Long memberId) {
        log.info("로그아웃 요청: memberId={}", memberId);
        refreshTokenRepository.revokeAllByMemberId(memberId);
        log.info("로그아웃 완료: memberId={}", memberId);
    }

    /**
     * 약관 동의 처리
     */
    public ConsentResponse submitConsent(Long memberId, ConsentRequest request) {
        log.info("약관 동의 요청: memberId={}", memberId);

        if (!Boolean.TRUE.equals(request.termsOfService()) || !Boolean.TRUE.equals(request.privacyPolicy())) {
            throw new BusinessException(ErrorCode.CONSENT_REQUIRED_FIELDS);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        MemberConsent consent = MemberConsent.builder()
                .member(member)
                .termsOfService(request.termsOfService())
                .privacyPolicy(request.privacyPolicy())
                .medicalDataConsent(request.medicalDataConsent())
                .marketingConsent(request.marketingConsent() != null ? request.marketingConsent() : false)
                .consentVersion(request.consentVersion())
                .consentedAt(LocalDateTime.now())
                .build();

        memberConsentRepository.save(consent);
        log.info("약관 동의 완료: memberId={}", memberId);

        return ConsentResponse.from(consent);
    }

    /**
     * 약관 동의 내역 조회
     */
    public ConsentResponse getConsent(Long memberId) {
        log.info("약관 동의 내역 조회: memberId={}", memberId);

        MemberConsent consent = memberConsentRepository.findTopByMemberIdOrderByConsentedAtDesc(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "약관 동의 내역이 없습니다."));

        return ConsentResponse.from(consent);
    }

    /**
     * 역할 목록 조회
     */
    public List<RoleResponse> getRoles() {
        log.info("역할 목록 조회");
        return roleRepository.findAllWithPermissions().stream()
                .map(RoleResponse::from)
                .toList();
    }

    /**
     * 사용자 역할 변경
     */
    public RoleChangeResponse changeUserRole(Long adminId, Long targetUserId, RoleChangeRequest request) {
        log.info("역할 변경 요청: adminId={}, targetUserId={}, newRoleId={}", adminId, targetUserId, request.roleId());

        if (adminId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_CHANGE_OWN_ROLE);
        }

        Member targetMember = memberRepository.findByIdWithRole(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String previousRoleName = targetMember.getRole().getName();

        Role newRole = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        targetMember.updateRole(newRole);
        log.info("역할 변경 완료: userId={}, {} -> {}", targetUserId, previousRoleName, newRole.getName());

        return new RoleChangeResponse(targetUserId, previousRoleName, newRole.getName());
    }

    /**
     * Refresh Token 반환 (Controller에서 Cookie 설정에 사용)
     */
    public String getRefreshTokenForMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        String refreshTokenStr = jwtTokenProvider.createRefreshToken(memberId);
        saveRefreshToken(member, refreshTokenStr);
        return refreshTokenStr;
    }

    // ===== Private helpers =====

    private void validateOAuthProvider(String provider) {
        if (!"google".equalsIgnoreCase(provider) && !"apple".equalsIgnoreCase(provider)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
    }

    /**
     * MVP 간소화: idToken에서 이메일 추출 (실제로는 Google/Apple 검증 필요)
     */
    private String extractEmailFromIdToken(String idToken) {
        // TODO: 실제 Google/Apple idToken 파싱 및 검증 로직 구현
        // MVP에서는 idToken 자체를 식별자로 사용하고, 테스트용 이메일 생성
        return "user_" + idToken.hashCode() + "@oauth.temp";
    }

    private String extractNameFromIdToken(String idToken) {
        // TODO: 실제 Google/Apple idToken에서 이름 추출
        return "OAuth User";
    }

    private void checkRateLimit(String target, MagicLinkTargetType targetType) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long recentCount;

        if (targetType == MagicLinkTargetType.EMAIL) {
            recentCount = magicLinkRepository.countRecentByTargetEmail(target, oneMinuteAgo);
        } else {
            recentCount = magicLinkRepository.countRecentByTargetPhone(target, oneMinuteAgo);
        }

        if (recentCount >= MAGIC_LINK_RATE_LIMIT) {
            throw new BusinessException(ErrorCode.MAGIC_LINK_RATE_LIMIT);
        }
    }

    private void saveRefreshToken(Member member, String tokenStr) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenStr)
                .member(member)
                .expiresAt(LocalDateTime.now().plusNanos(jwtProperties.refreshTokenExpiration() * 1_000_000))
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    private String maskTarget(String target) {
        if (target == null || target.length() < 4) {
            return "***";
        }
        if (target.contains("@")) {
            String[] parts = target.split("@");
            String local = parts[0];
            String masked = local.substring(0, Math.min(3, local.length())) + "***";
            return masked + "@" + parts[1];
        }
        return target.substring(0, 3) + "***" + target.substring(target.length() - 2);
    }
}
