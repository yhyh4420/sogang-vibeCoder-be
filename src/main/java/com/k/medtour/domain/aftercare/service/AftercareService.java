package com.k.medtour.domain.aftercare.service;

import com.k.medtour.domain.aftercare.dto.AftercareGuideCreateRequest;
import com.k.medtour.domain.aftercare.dto.AftercareGuideResponse;
import com.k.medtour.domain.aftercare.dto.InvoiceCreateRequest;
import com.k.medtour.domain.aftercare.dto.InvoiceResponse;
import com.k.medtour.domain.aftercare.dto.StaffReportCreateRequest;
import com.k.medtour.domain.aftercare.dto.StaffReportResponse;
import com.k.medtour.domain.aftercare.entity.AftercareGuide;
import com.k.medtour.domain.aftercare.entity.Invoice;
import com.k.medtour.domain.aftercare.entity.InvoiceItem;
import com.k.medtour.domain.aftercare.entity.StaffReport;
import com.k.medtour.domain.aftercare.repository.AftercareGuideRepository;
import com.k.medtour.domain.aftercare.repository.InvoiceRepository;
import com.k.medtour.domain.aftercare.repository.StaffReportRepository;
import com.k.medtour.domain.journey.repository.JourneyRepository;
import com.k.medtour.global.exception.BusinessException;
import com.k.medtour.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
public class AftercareService {

    private final AftercareGuideRepository guideRepository;
    private final InvoiceRepository invoiceRepository;
    private final StaffReportRepository staffReportRepository;
    private final JourneyRepository journeyRepository;

    public AftercareGuideResponse createGuide(AftercareGuideCreateRequest request) {
        journeyRepository.findByIdAndDeletedAtIsNull(request.journeyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.JOURNEY_NOT_FOUND));

        if (guideRepository.existsByJourneyIdAndDeletedAtIsNull(request.journeyId())) {
            throw new BusinessException(ErrorCode.GUIDE_ALREADY_EXISTS);
        }

        AftercareGuide guide = AftercareGuide.builder()
                .journeyId(request.journeyId())
                .title(request.title())
                .content(request.content())
                .instructions(request.instructions())
                .build();

        return AftercareGuideResponse.from(guideRepository.save(guide));
    }

    public AftercareGuideResponse getGuide(Long journeyId) {
        AftercareGuide guide = guideRepository.findByJourneyIdAndDeletedAtIsNull(journeyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GUIDE_NOT_FOUND));
        return AftercareGuideResponse.from(guide);
    }

    public InvoiceResponse createInvoice(InvoiceCreateRequest request) {
        journeyRepository.findByIdAndDeletedAtIsNull(request.journeyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.JOURNEY_NOT_FOUND));

        if (invoiceRepository.existsByJourneyIdAndDeletedAtIsNull(request.journeyId())) {
            throw new BusinessException(ErrorCode.INVOICE_ALREADY_EXISTS);
        }

        String invoiceNumber = generateInvoiceNumber();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (InvoiceCreateRequest.InvoiceItemRequest item : request.items()) {
            BigDecimal itemAmount = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            subtotal = subtotal.add(itemAmount);
        }

        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.1)).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(tax);

        Invoice invoice = Invoice.builder()
                .journeyId(request.journeyId())
                .patientId(request.patientId())
                .invoiceNumber(invoiceNumber)
                .currency(request.currency())
                .subtotal(subtotal)
                .tax(tax)
                .totalAmount(totalAmount)
                .dueDate(request.dueDate())
                .build();

        for (InvoiceCreateRequest.InvoiceItemRequest itemReq : request.items()) {
            BigDecimal itemAmount = itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .description(itemReq.description())
                    .unitPrice(itemReq.unitPrice())
                    .quantity(itemReq.quantity())
                    .amount(itemAmount)
                    .build();
            invoice.addItem(item);
        }

        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    public InvoiceResponse getInvoiceByJourneyId(Long journeyId) {
        Invoice invoice = invoiceRepository.findByJourneyIdWithItems(journeyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND));
        return InvoiceResponse.from(invoice);
    }

    public List<InvoiceResponse> getMyInvoices(Long patientId) {
        return invoiceRepository.findByPatientIdWithItems(patientId).stream()
                .map(InvoiceResponse::from)
                .toList();
    }

    public StaffReportResponse createReport(StaffReportCreateRequest request, Long staffId) {
        journeyRepository.findByIdAndDeletedAtIsNull(request.journeyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.JOURNEY_NOT_FOUND));

        if (staffReportRepository.existsByJourneyIdAndStaffIdAndDeletedAtIsNull(
                request.journeyId(), staffId)) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_EXISTS);
        }

        StaffReport report = StaffReport.builder()
                .journeyId(request.journeyId())
                .staffId(staffId)
                .reportContent(request.reportContent())
                .workHours(request.workHours())
                .build();

        return StaffReportResponse.from(staffReportRepository.save(report));
    }

    private String generateInvoiceNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = invoiceRepository.countAllActive() + 1;
        return String.format("INV-%s-%04d", datePrefix, count);
    }
}
