package com.k.medtour.domain.proposal.entity;

import com.k.medtour.domain.proposal.enums.ProposalItemCategory;
import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProposalItem extends BaseEntity {



    private Proposal proposal;



    private ProposalItemCategory category;


    private String name;


    private String description;


    private BigDecimal unitPrice;


    private Integer quantity;


    private BigDecimal amount;

    @Builder
    public ProposalItem(ProposalItemCategory category, String name, String description,
                        BigDecimal unitPrice, Integer quantity) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.amount = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    void assignProposal(Proposal proposal) {
        this.proposal = proposal;
    }
}
