package com.k.medtour.domain.proposal.repository;

import com.k.medtour.domain.proposal.entity.Proposal;
import com.k.medtour.domain.proposal.enums.ProposalStatus;

import java.util.List;
import java.util.Optional;

public interface ProposalRepository {

    Proposal save(Proposal proposal);

    Optional<Proposal> findById(Long id);

    List<Proposal> findAllWithFilters(ProposalStatus status, Long patientId, int page, int size);

    long countByFilters(ProposalStatus status, Long patientId);

    List<Proposal> findByPatientIdWithFilter(Long patientId, ProposalStatus status, int page, int size);

    long countByPatientIdWithFilter(Long patientId, ProposalStatus status);
}
