package com.k.medtour.domain.patient.entity;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmergencyContact extends BaseEntity {



    private Member member;


    private String name;


    private String relationship;


    private String phone;


    private String email;


    private Boolean isPrimary = false;

    @Builder
    public EmergencyContact(Member member, String name, String relationship,
                            String phone, String email, Boolean isPrimary) {
        this.member = member;
        this.name = name;
        this.relationship = relationship;
        this.phone = phone;
        this.email = email;
        this.isPrimary = isPrimary != null ? isPrimary : false;
    }

    public void update(String name, String relationship, String phone,
                       String email, Boolean isPrimary) {
        this.name = name;
        this.relationship = relationship;
        this.phone = phone;
        this.email = email;
        if (isPrimary != null) this.isPrimary = isPrimary;
    }
}
