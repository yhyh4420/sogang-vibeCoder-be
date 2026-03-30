package com.k.medtour.domain.proposal.entity;

import com.k.medtour.domain.proposal.enums.ProposalStatus;
import com.k.medtour.global.common.BaseEntity;
import com.k.medtour.global.exception.BusinessException;
import com.k.medtour.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Proposal extends BaseEntity {


    private Long patientId;


    private String title;



    private ProposalStatus status;


    private String currency;


    private BigDecimal subtotal;


    private BigDecimal discountRate;


    private BigDecimal discountAmount;


    private BigDecimal totalAmount;


    private LocalDateTime validUntil;


    private String notes;


    private LocalDateTime sentAt;


    private LocalDateTime respondedAt;


    private List<ProposalItem> items = new ArrayList<>();

    @Builder
    public Proposal(Long patientId, String title, String currency, BigDecimal discountRate,
                    LocalDateTime validUntil, String notes) {
        this.patientId = patientId;
        this.title = title;
        this.status = ProposalStatus.DRAFT;
        this.currency = currency;
        this.discountRate = discountRate != null ? discountRate : BigDecimal.ZERO;
        this.validUntil = validUntil;
        this.notes = notes;
        this.subtotal = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }

    public void addItem(ProposalItem item) {
        this.items.add(item);
        item.assignProposal(this);
    }

    public void clearItems() {
        this.items.clear();
    }

    public void calculateAmounts() {
        this.subtotal = items.stream()
                .map(ProposalItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (this.discountRate != null && this.discountRate.compareTo(BigDecimal.ZERO) > 0) {
            this.discountAmount = this.subtotal
                    .multiply(this.discountRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            this.discountAmount = BigDecimal.ZERO;
        }

        this.totalAmount = this.subtotal.subtract(this.discountAmount);
    }

    public void send() {
        validateStatus(ProposalStatus.DRAFT, "DRAFT 상태의 견적서만 발송할 수 있습니다.");
        this.status = ProposalStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    public void accept() {
        validateStatus(ProposalStatus.SENT, "SENT 상태의 견적서만 수락할 수 있습니다.");
        this.status = ProposalStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        validateStatus(ProposalStatus.SENT, "SENT 상태의 견적서만 거절할 수 있습니다.");
        this.status = ProposalStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void updateDraft(String title, String currency, BigDecimal discountRate,
                            LocalDateTime validUntil, String notes) {
        validateStatus(ProposalStatus.DRAFT, "DRAFT 상태의 견적서만 수정할 수 있습니다.");
        if (title != null) this.title = title;
        if (currency != null) this.currency = currency;
        if (discountRate != null) this.discountRate = discountRate;
        if (validUntil != null) this.validUntil = validUntil;
        if (notes != null) this.notes = notes;
    }

    private void validateStatus(ProposalStatus expected, String message) {
        if (this.status != expected) {
            throw new BusinessException(ErrorCode.PROPOSAL_INVALID_STATUS, message);
        }
    }

    public boolean isRespondable() {
        return this.status == ProposalStatus.SENT && this.respondedAt == null;
    }
}
