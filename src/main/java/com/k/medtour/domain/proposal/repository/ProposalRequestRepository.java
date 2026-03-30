package com.k.medtour.domain.proposal.repository;

import com.k.medtour.domain.proposal.entity.ProposalRequest;

import java.util.List;
import java.util.Optional;

public interface ProposalRequestRepository {

    ProposalRequest save(ProposalRequest proposalRequest);

    Optional<ProposalRequest> findById(Long id);

    List<ProposalRequest> findByPatientId(Long patientId);
}
