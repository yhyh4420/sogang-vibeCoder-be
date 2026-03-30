package com.k.medtour.domain.proposal.repository;

import com.k.medtour.domain.proposal.entity.ProposalItem;

import java.util.List;

public interface ProposalItemRepository {

    ProposalItem save(ProposalItem proposalItem);

    List<ProposalItem> findByProposalId(Long proposalId);
}
