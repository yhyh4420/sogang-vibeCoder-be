package com.k.medtour.domain.admin.entity;

import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {


    private String email;


    private String name;



    private Role role;


    private String oauthProvider;


    private String oauthId;


    private String phone;


    private String language = "en";


    private String profileImage;

    @Builder
    public Member(String email, String name, Role role, String oauthProvider,
                  String oauthId, String phone, String language, String profileImage) {
        this.email = email;
        this.name = name;
        this.role = role;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
        this.phone = phone;
        this.language = language != null ? language : "en";
        this.profileImage = profileImage;
    }

    public void updateRole(Role role) {
        this.role = role;
    }

    public void updateProfile(String name, String phone, String language, String profileImage) {
        if (name != null) this.name = name;
        if (phone != null) this.phone = phone;
        if (language != null) this.language = language;
        if (profileImage != null) this.profileImage = profileImage;
    }
}
