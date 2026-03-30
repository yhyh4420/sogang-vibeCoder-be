package com.k.medtour.domain.aftercare.entity;

import com.k.medtour.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvoiceItem extends BaseEntity {



    private Invoice invoice;


    private String description;


    private BigDecimal unitPrice;


    private Integer quantity;


    private BigDecimal amount;

    @Builder
    public InvoiceItem(Invoice invoice, String description, BigDecimal unitPrice,
                       Integer quantity, BigDecimal amount) {
        this.invoice = invoice;
        this.description = description;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.amount = amount;
    }
}
