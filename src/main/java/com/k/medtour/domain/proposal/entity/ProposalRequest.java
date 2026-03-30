package com.k.medtour.domain.proposal.entity;

import com.k.medtour.domain.proposal.enums.ProposalRequestStatus;
import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProposalRequest extends BaseEntity {


    private Long patientId;



    private List<String> desiredProcedures;



    private List<Long> preferredHospitalIds;


    private LocalDate arrivalDate;


    private LocalDate departureDate;


    private String accommodationPreference;



    private List<String> conciergeServices;


    private BigDecimal budgetMin;


    private BigDecimal budgetMax;


    private String budgetCurrency;


    private String additionalRequests;



    private ProposalRequestStatus status;

    @Builder
    public ProposalRequest(Long patientId, List<String> desiredProcedures, List<Long> preferredHospitalIds,
                           LocalDate arrivalDate, LocalDate departureDate, String accommodationPreference,
                           List<String> conciergeServices, BigDecimal budgetMin, BigDecimal budgetMax,
                           String budgetCurrency, String additionalRequests) {
        this.patientId = patientId;
        this.desiredProcedures = desiredProcedures;
        this.preferredHospitalIds = preferredHospitalIds;
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
        this.accommodationPreference = accommodationPreference;
        this.conciergeServices = conciergeServices;
        this.budgetMin = budgetMin;
        this.budgetMax = budgetMax;
        this.budgetCurrency = budgetCurrency;
        this.additionalRequests = additionalRequests;
        this.status = ProposalRequestStatus.PENDING;
    }
}
