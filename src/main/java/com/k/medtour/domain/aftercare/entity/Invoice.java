package com.k.medtour.domain.aftercare.entity;

import com.k.medtour.domain.aftercare.enums.InvoiceStatus;
import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invoice extends BaseEntity {


    private Long journeyId;


    private Long patientId;


    private String invoiceNumber;


    private String currency;


    private BigDecimal subtotal;


    private BigDecimal tax;


    private BigDecimal totalAmount;



    private InvoiceStatus status = InvoiceStatus.DRAFT;


    private LocalDateTime issuedAt;


    private LocalDate dueDate;


    private List<InvoiceItem> items = new ArrayList<>();

    @Builder
    public Invoice(Long journeyId, Long patientId, String invoiceNumber, String currency,
                   BigDecimal subtotal, BigDecimal tax, BigDecimal totalAmount,
                   LocalDate dueDate) {
        this.journeyId = journeyId;
        this.patientId = patientId;
        this.invoiceNumber = invoiceNumber;
        this.currency = currency;
        this.subtotal = subtotal;
        this.tax = tax;
        this.totalAmount = totalAmount;
        this.status = InvoiceStatus.DRAFT;
        this.issuedAt = LocalDateTime.now();
        this.dueDate = dueDate;
    }

    public void addItem(InvoiceItem item) {
        this.items.add(item);
    }

    public void send() {
        this.status = InvoiceStatus.SENT;
    }

    public void markAsPaid() {
        this.status = InvoiceStatus.PAID;
    }
}
