package com.k.medtour.domain.admin.repository;

import com.k.medtour.domain.admin.entity.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {

    Role save(Role role);

    Optional<Role> findById(Long id);

    Optional<Role> findByName(String name);

    List<Role> findAllWithPermissions();

    Optional<Role> findByIdWithPermissions(Long id);
}
