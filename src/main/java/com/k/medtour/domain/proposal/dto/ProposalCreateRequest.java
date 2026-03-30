package com.k.medtour.domain.proposal.dto;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProposalCreateRequest(
        Long patientId,

        String title,

        String currency,

        LocalDateTime validUntil,

        List<ProposalItemDto> items,

        BigDecimal discountRate,

        String notes
) {
}
