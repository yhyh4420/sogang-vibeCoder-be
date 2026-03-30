package com.k.medtour.domain.proposal.dto;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProposalRequestCreateRequest(
        List<String> desiredProcedures,

        List<Long> preferredHospitalIds,

        LocalDate arrivalDate,

        LocalDate departureDate,

        String accommodationPreference,

        List<String> conciergeServices,

        BigDecimal budgetMin,
        BigDecimal budgetMax,
        String budgetCurrency,

        String additionalRequests
) {
}
