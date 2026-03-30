package com.k.medtour.domain.admin.entity;

import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgencyProfile extends BaseEntity {


    private String name;


    private String licenseNumber;


    private Boolean licenseVerified = false;


    private String address;


    private String phone;


    private String website;


    private String description;

    @Builder
    public AgencyProfile(String name, String licenseNumber, Boolean licenseVerified,
                         String address, String phone, String website, String description) {
        this.name = name;
        this.licenseNumber = licenseNumber;
        this.licenseVerified = licenseVerified != null ? licenseVerified : false;
        this.address = address;
        this.phone = phone;
        this.website = website;
        this.description = description;
    }

    public void updateProfile(String name, String address, String phone,
                              String website, String description) {
        if (name != null) this.name = name;
        if (address != null) this.address = address;
        if (phone != null) this.phone = phone;
        if (website != null) this.website = website;
        if (description != null) this.description = description;
    }

    public void verifyLicense() {
        this.licenseVerified = true;
    }

    public void unverifyLicense() {
        this.licenseVerified = false;
    }
}
