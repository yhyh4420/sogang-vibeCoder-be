package com.k.medtour.domain.staff.repository;

import com.k.medtour.domain.staff.entity.StaffProfile;

import java.util.List;
import java.util.Optional;

public interface StaffProfileRepository {

    StaffProfile save(StaffProfile staffProfile);

    Optional<StaffProfile> findById(Long id);

    Optional<StaffProfile> findByMemberIdWithMember(Long memberId);

    Optional<StaffProfile> findByMemberId(Long memberId);

    List<StaffProfile> findAll();
}
