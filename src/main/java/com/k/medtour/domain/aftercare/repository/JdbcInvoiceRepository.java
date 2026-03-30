package com.k.medtour.domain.aftercare.repository;

import com.k.medtour.domain.aftercare.entity.Invoice;
import com.k.medtour.domain.aftercare.entity.InvoiceItem;
import com.k.medtour.domain.aftercare.enums.InvoiceStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcInvoiceRepository implements InvoiceRepository {

    private final DataSource dataSource;

    public JdbcInvoiceRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Invoice save(Invoice inv) {
        if (inv.getId() == null) return insert(inv);
        else return update(inv);
    }

    private Invoice insert(Invoice inv) {
        inv.prePersist();
        String sql = """
            INSERT INTO invoice (journey_id, patient_id, invoice_number, currency, subtotal, tax,
                                 total_amount, status, issued_at, due_date, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, inv.getJourneyId());
            stmt.setLong(2, inv.getPatientId());
            stmt.setString(3, inv.getInvoiceNumber());
            stmt.setString(4, inv.getCurrency());
            stmt.setBigDecimal(5, inv.getSubtotal());
            stmt.setBigDecimal(6, inv.getTax());
            stmt.setBigDecimal(7, inv.getTotalAmount());
            stmt.setString(8, inv.getStatus().name());
            stmt.setObject(9, inv.getIssuedAt() != null ? Timestamp.valueOf(inv.getIssuedAt()) : null);
            stmt.setObject(10, inv.getDueDate());
            stmt.setTimestamp(11, Timestamp.valueOf(inv.getCreatedAt()));
            stmt.setTimestamp(12, Timestamp.valueOf(inv.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) { if (rs.next()) inv.setId(rs.getLong(1)); }
            for (InvoiceItem item : inv.getItems()) {
                saveItem(item, inv.getId(), conn);
            }
            return inv;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private void saveItem(InvoiceItem item, Long invoiceId, Connection conn) throws SQLException {
        item.prePersist();
        String sql = """
            INSERT INTO invoice_item (invoice_id, description, unit_price, quantity, amount, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, invoiceId);
            stmt.setString(2, item.getDescription());
            stmt.setBigDecimal(3, item.getUnitPrice());
            stmt.setInt(4, item.getQuantity());
            stmt.setBigDecimal(5, item.getAmount());
            stmt.setTimestamp(6, Timestamp.valueOf(item.getCreatedAt()));
            stmt.setTimestamp(7, Timestamp.valueOf(item.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) { if (rs.next()) item.setId(rs.getLong(1)); }
        }
    }

    private Invoice update(Invoice inv) {
        inv.preUpdate();
        String sql = "UPDATE invoice SET status = ?, updated_at = ? WHERE id = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, inv.getStatus().name());
            stmt.setTimestamp(2, Timestamp.valueOf(inv.getUpdatedAt()));
            stmt.setLong(3, inv.getId());
            stmt.executeUpdate();
            return inv;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public Optional<Invoice> findById(Long id) {
        String sql = "SELECT * FROM invoice WHERE id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) { Invoice inv = mapRow(rs); loadItems(inv, conn); return Optional.of(inv); }
                return Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public Optional<Invoice> findByJourneyIdWithItems(Long journeyId) {
        String sql = "SELECT * FROM invoice WHERE journey_id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, journeyId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) { Invoice inv = mapRow(rs); loadItems(inv, conn); return Optional.of(inv); }
                return Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public boolean existsByJourneyIdAndDeletedAtIsNull(Long journeyId) {
        String sql = "SELECT COUNT(*) FROM invoice WHERE journey_id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, journeyId);
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1) > 0; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public List<Invoice> findByPatientIdWithItems(Long patientId) {
        String sql = "SELECT * FROM invoice WHERE patient_id = ? AND deleted_at IS NULL ORDER BY created_at DESC";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, patientId);
            try (var rs = stmt.executeQuery()) {
                List<Invoice> list = new ArrayList<>();
                while (rs.next()) { Invoice inv = mapRow(rs); loadItems(inv, conn); list.add(inv); }
                return list;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public long countAllActive() {
        String sql = "SELECT COUNT(*) FROM invoice WHERE deleted_at IS NULL";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1); }
        catch (SQLException e) { throw new RuntimeException(e); }
    }

    private void loadItems(Invoice inv, Connection conn) throws SQLException {
        String sql = "SELECT * FROM invoice_item WHERE invoice_id = ? ORDER BY id";
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, inv.getId());
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    InvoiceItem item = InvoiceItem.builder()
                            .invoice(inv)
                            .description(rs.getString("description"))
                            .unitPrice(rs.getBigDecimal("unit_price"))
                            .quantity(rs.getInt("quantity"))
                            .amount(rs.getBigDecimal("amount"))
                            .build();
                    item.setId(rs.getLong("id"));
                    inv.addItem(item);
                }
            }
        }
    }

    private Invoice mapRow(ResultSet rs) throws SQLException {
        Invoice inv = Invoice.builder()
                .journeyId(rs.getLong("journey_id"))
                .patientId(rs.getLong("patient_id"))
                .invoiceNumber(rs.getString("invoice_number"))
                .currency(rs.getString("currency"))
                .subtotal(rs.getBigDecimal("subtotal"))
                .tax(rs.getBigDecimal("tax"))
                .totalAmount(rs.getBigDecimal("total_amount"))
                .dueDate(rs.getObject("due_date", LocalDate.class))
                .build();
        inv.setId(rs.getLong("id"));
        inv.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        inv.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        inv.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
        setField(inv, "status", InvoiceStatus.valueOf(rs.getString("status")));
        setField(inv, "issuedAt", toLocalDateTime(rs.getTimestamp("issued_at")));
        return inv;
    }

    private void setField(Object obj, String name, Object value) {
        try { var f = obj.getClass().getDeclaredField(name); f.setAccessible(true); f.set(obj, value); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) { return ts != null ? ts.toLocalDateTime() : null; }
}
