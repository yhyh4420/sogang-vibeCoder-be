package com.k.medtour.domain.proposal.dto;

import com.k.medtour.domain.proposal.entity.ProposalItem;
import com.k.medtour.domain.proposal.enums.ProposalItemCategory;

import java.math.BigDecimal;

public record ProposalItemDto(
        Long id,

        ProposalItemCategory category,

        String name,

        String description,

        BigDecimal unitPrice,

        Integer quantity,

        BigDecimal amount
) {
    public static ProposalItemDto from(ProposalItem entity) {
        return new ProposalItemDto(
                entity.getId(),
                entity.getCategory(),
                entity.getName(),
                entity.getDescription(),
                entity.getUnitPrice(),
                entity.getQuantity(),
                entity.getAmount()
        );
    }

    public ProposalItem toEntity() {
        return ProposalItem.builder()
                .category(category)
                .name(name)
                .description(description)
                .unitPrice(unitPrice)
                .quantity(quantity)
                .build();
    }
}
