package com.k.medtour.domain.admin.repository;

import com.k.medtour.domain.admin.entity.AgencyProfile;

import java.util.List;
import java.util.Optional;

public interface AgencyProfileRepository {

    AgencyProfile save(AgencyProfile agencyProfile);

    Optional<AgencyProfile> findById(Long id);

    Optional<AgencyProfile> findByLicenseNumber(String licenseNumber);

    List<AgencyProfile> findAll();
}
