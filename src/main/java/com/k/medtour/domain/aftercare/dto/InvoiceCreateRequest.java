package com.k.medtour.domain.aftercare.dto;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceCreateRequest(
        Long journeyId,
        Long patientId,
        String currency,
        LocalDate dueDate,
        List<InvoiceItemRequest> items
) {
    public record InvoiceItemRequest(
            String description,
            BigDecimal unitPrice,
            Integer quantity
    ) {
    }
}
