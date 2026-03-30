package com.k.medtour.domain.aftercare.repository;

import com.k.medtour.domain.aftercare.entity.Invoice;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(Long id);

    Optional<Invoice> findByJourneyIdWithItems(Long journeyId);

    boolean existsByJourneyIdAndDeletedAtIsNull(Long journeyId);

    List<Invoice> findByPatientIdWithItems(Long patientId);

    long countAllActive();
}
