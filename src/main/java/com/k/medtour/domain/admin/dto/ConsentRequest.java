package com.k.medtour.domain.admin.dto;


public record ConsentRequest(
        Boolean termsOfService,

        Boolean privacyPolicy,

        Boolean medicalDataConsent,

        Boolean marketingConsent,

        String consentVersion
) {
}
