package com.k.medtour.domain.admin.repository;

import com.k.medtour.domain.admin.entity.Member;

import java.util.List;
import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long id);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);

    boolean existsByEmail(String email);

    Optional<Member> findByIdWithRole(Long id);

    Optional<Member> findByEmailWithRole(String email);

    List<Member> findAllByRoleName(String roleName, int page, int size);

    long countByRoleName(String roleName);
}
