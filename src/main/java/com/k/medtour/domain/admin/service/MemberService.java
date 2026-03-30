package com.k.medtour.domain.admin.service;

import com.k.medtour.domain.admin.dto.*;
import com.k.medtour.domain.admin.entity.AgencyProfile;
import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.admin.repository.AgencyProfileRepository;
import com.k.medtour.domain.admin.repository.MemberRepository;
import com.k.medtour.domain.staff.entity.StaffProfile;
import com.k.medtour.domain.staff.repository.StaffProfileRepository;
import com.k.medtour.global.exception.BusinessException;
import com.k.medtour.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final AgencyProfileRepository agencyProfileRepository;

    /**
     * 실무자 본인 프로필 조회
     */
    public StaffProfileResponse getStaffProfile(Long memberId) {
        log.info("실무자 프로필 조회: memberId={}", memberId);

        StaffProfile staffProfile = staffProfileRepository.findByMemberIdWithMember(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STAFF_PROFILE_NOT_FOUND));

        return StaffProfileResponse.from(staffProfile);
    }

    /**
     * 실무자 본인 프로필 수정
     */
    public StaffProfileResponse updateStaffProfile(Long memberId, StaffProfileUpdateRequest request) {
        log.info("실무자 프로필 수정: memberId={}", memberId);

        StaffProfile staffProfile = staffProfileRepository.findByMemberIdWithMember(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STAFF_PROFILE_NOT_FOUND));

        Member member = staffProfile.getMember();
        member.updateProfile(request.name(), request.phone(), null, request.profileImageUrl());

        staffProfile.updateProfile(null, request.languages(), request.vehicleInfo());

        log.info("실무자 프로필 수정 완료: memberId={}", memberId);
        return StaffProfileResponse.from(staffProfile);
    }

    /**
     * 에이전시 프로필 조회 (첫 번째 에이전시 프로필 반환 -- MVP 단일 에이전시 가정)
     */
    public AgencyProfileResponse getAgencyProfile() {
        log.info("에이전시 프로필 조회");

        AgencyProfile profile = agencyProfileRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENCY_PROFILE_NOT_FOUND));

        return AgencyProfileResponse.from(profile);
    }

    /**
     * 에이전시 프로필 수정
     */
    public AgencyProfileResponse updateAgencyProfile(AgencyProfileUpdateRequest request) {
        log.info("에이전시 프로필 수정 요청");

        AgencyProfile profile = agencyProfileRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENCY_PROFILE_NOT_FOUND));

        profile.updateProfile(
                request.name(),
                request.address(),
                request.phone(),
                request.website(),
                request.description()
        );

        log.info("에이전시 프로필 수정 완료: profileId={}", profile.getId());
        return AgencyProfileResponse.from(profile);
    }

    /**
     * 에이전시 라이선스 검증
     */
    public LicenseVerifyResponse verifyLicense() {
        log.info("에이전시 라이선스 검증 조회");

        AgencyProfile profile = agencyProfileRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENCY_PROFILE_NOT_FOUND));

        return LicenseVerifyResponse.from(profile);
    }
}
