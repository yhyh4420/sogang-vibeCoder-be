package com.k.medtour.domain.proposal.controller;

import com.k.medtour.domain.proposal.dto.ProposalAcceptResponse;
import com.k.medtour.domain.proposal.dto.ProposalCreateRequest;
import com.k.medtour.domain.proposal.dto.ProposalListResponse;
import com.k.medtour.domain.proposal.dto.ProposalRejectResponse;
import com.k.medtour.domain.proposal.dto.ProposalRequestCreateRequest;
import com.k.medtour.domain.proposal.dto.ProposalRequestResponse;
import com.k.medtour.domain.proposal.dto.ProposalResponse;
import com.k.medtour.domain.proposal.dto.ProposalSendResponse;
import com.k.medtour.domain.proposal.enums.ProposalStatus;
import com.k.medtour.domain.proposal.service.ProposalService;
import com.k.medtour.global.auth.UserPrincipal;
import com.k.medtour.global.common.ApiResponse;
import com.k.medtour.global.common.PageResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    public ApiResponse<ProposalResponse> createProposal(
            ProposalCreateRequest request) {
        ProposalResponse response = proposalService.createProposal(request);
        return ApiResponse.success("견적서 생성 완료", response);
    }

    public ApiResponse<PageResponse<ProposalListResponse>> getProposals(
            ProposalStatus status,
            Long patientId,
            int page,
            int size) {
        PageResponse<ProposalListResponse> response = proposalService.getProposals(status, patientId, page, size);
        return ApiResponse.success("조회 성공", response);
    }

    public ApiResponse<ProposalResponse> getProposal(
            Long proposalId,
            UserPrincipal principal) {
        ProposalResponse response = proposalService.getProposal(
                proposalId, principal.memberId(), principal.role());
        return ApiResponse.success("조회 성공", response);
    }

    public ApiResponse<ProposalSendResponse> sendProposal(Long proposalId) {
        ProposalSendResponse response = proposalService.sendProposal(proposalId);
        return ApiResponse.success("견적서 발송 완료", response);
    }

    public ApiResponse<ProposalRequestResponse> createProposalRequest(
            ProposalRequestCreateRequest request,
            UserPrincipal principal) {
        ProposalRequestResponse response = proposalService.createProposalRequest(
                principal.memberId(), request);
        return ApiResponse.success("견적 요청 완료", response);
    }

    public ApiResponse<PageResponse<ProposalListResponse>> getMyProposals(
            ProposalStatus status,
            int page,
            int size,
            UserPrincipal principal) {
        PageResponse<ProposalListResponse> response = proposalService.getMyProposals(
                principal.memberId(), status, page, size);
        return ApiResponse.success("조회 성공", response);
    }

    public ApiResponse<ProposalAcceptResponse> acceptProposal(
            Long proposalId,
            UserPrincipal principal) {
        ProposalAcceptResponse response = proposalService.acceptProposal(
                proposalId, principal.memberId());
        return ApiResponse.success("견적서 수락 완료", response);
    }

    public ApiResponse<ProposalRejectResponse> rejectProposal(
            Long proposalId,
            UserPrincipal principal) {
        ProposalRejectResponse response = proposalService.rejectProposal(
                proposalId, principal.memberId());
        return ApiResponse.success("견적서 거절 완료", response);
    }
}
