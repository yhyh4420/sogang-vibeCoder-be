package com.k.medtour.domain.admin.entity;

import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberConsent extends BaseEntity {



    private Member member;


    private Boolean termsOfService = false;


    private Boolean privacyPolicy = false;


    private Boolean medicalDataConsent = false;


    private Boolean marketingConsent = false;


    private String consentVersion;


    private LocalDateTime consentedAt;

    @Builder
    public MemberConsent(Member member, Boolean termsOfService, Boolean privacyPolicy,
                         Boolean medicalDataConsent, Boolean marketingConsent,
                         String consentVersion, LocalDateTime consentedAt) {
        this.member = member;
        this.termsOfService = termsOfService != null ? termsOfService : false;
        this.privacyPolicy = privacyPolicy != null ? privacyPolicy : false;
        this.medicalDataConsent = medicalDataConsent != null ? medicalDataConsent : false;
        this.marketingConsent = marketingConsent != null ? marketingConsent : false;
        this.consentVersion = consentVersion;
        this.consentedAt = consentedAt != null ? consentedAt : LocalDateTime.now();
    }
}
