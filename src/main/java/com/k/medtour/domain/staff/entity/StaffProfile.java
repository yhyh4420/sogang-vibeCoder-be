package com.k.medtour.domain.staff.entity;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.staff.enums.StaffType;
import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffProfile extends BaseEntity {


    private Member member;



    private StaffType staffType;



    private List<String> languages;



    private Map<String, Object> vehicleInfo;


    private Boolean isAvailable = true;

    @Builder
    public StaffProfile(Member member, StaffType staffType, List<String> languages,
                        Map<String, Object> vehicleInfo, Boolean isAvailable) {
        this.member = member;
        this.staffType = staffType;
        this.languages = languages;
        this.vehicleInfo = vehicleInfo;
        this.isAvailable = isAvailable != null ? isAvailable : true;
    }

    public void updateProfile(StaffType staffType, List<String> languages,
                              Map<String, Object> vehicleInfo) {
        if (staffType != null) this.staffType = staffType;
        if (languages != null) this.languages = languages;
        if (vehicleInfo != null) this.vehicleInfo = vehicleInfo;
    }

    public void updateAvailability(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
}
