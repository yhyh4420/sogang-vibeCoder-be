package com.k.medtour.domain.proposal.repository;

import com.k.medtour.domain.proposal.entity.Proposal;
import com.k.medtour.domain.proposal.entity.ProposalItem;
import com.k.medtour.domain.proposal.enums.ProposalItemCategory;
import com.k.medtour.domain.proposal.enums.ProposalStatus;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcProposalRepository implements ProposalRepository {

    private final DataSource dataSource;

    public JdbcProposalRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Proposal save(Proposal proposal) {
        if (proposal.getId() == null) return insert(proposal);
        else return update(proposal);
    }

    private Proposal insert(Proposal p) {
        p.prePersist();
        String sql = """
            INSERT INTO proposal (patient_id, title, status, currency, subtotal, discount_rate,
                                  discount_amount, total_amount, valid_until, notes, sent_at, responded_at,
                                  created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, p.getPatientId());
            stmt.setString(2, p.getTitle());
            stmt.setString(3, p.getStatus().name());
            stmt.setString(4, p.getCurrency());
            stmt.setBigDecimal(5, p.getSubtotal());
            stmt.setBigDecimal(6, p.getDiscountRate());
            stmt.setBigDecimal(7, p.getDiscountAmount());
            stmt.setBigDecimal(8, p.getTotalAmount());
            stmt.setObject(9, p.getValidUntil() != null ? Timestamp.valueOf(p.getValidUntil()) : null);
            stmt.setString(10, p.getNotes());
            stmt.setObject(11, p.getSentAt() != null ? Timestamp.valueOf(p.getSentAt()) : null);
            stmt.setObject(12, p.getRespondedAt() != null ? Timestamp.valueOf(p.getRespondedAt()) : null);
            stmt.setTimestamp(13, Timestamp.valueOf(p.getCreatedAt()));
            stmt.setTimestamp(14, Timestamp.valueOf(p.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) p.setId(rs.getLong(1));
            }
            // Save items
            for (ProposalItem item : p.getItems()) {
                saveItem(item, p.getId(), conn);
            }
            return p;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveItem(ProposalItem item, Long proposalId, Connection conn) throws SQLException {
        item.prePersist();
        String sql = """
            INSERT INTO proposal_item (proposal_id, category, name, description, unit_price, quantity, amount,
                                       created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, proposalId);
            stmt.setString(2, item.getCategory().name());
            stmt.setString(3, item.getName());
            stmt.setString(4, item.getDescription());
            stmt.setBigDecimal(5, item.getUnitPrice());
            stmt.setInt(6, item.getQuantity());
            stmt.setBigDecimal(7, item.getAmount());
            stmt.setTimestamp(8, Timestamp.valueOf(item.getCreatedAt()));
            stmt.setTimestamp(9, Timestamp.valueOf(item.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) item.setId(rs.getLong(1));
            }
        }
    }

    private Proposal update(Proposal p) {
        p.preUpdate();
        String sql = """
            UPDATE proposal SET status = ?, subtotal = ?, discount_rate = ?, discount_amount = ?,
                                total_amount = ?, sent_at = ?, responded_at = ?, updated_at = ?
            WHERE id = ?
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, p.getStatus().name());
            stmt.setBigDecimal(2, p.getSubtotal());
            stmt.setBigDecimal(3, p.getDiscountRate());
            stmt.setBigDecimal(4, p.getDiscountAmount());
            stmt.setBigDecimal(5, p.getTotalAmount());
            stmt.setObject(6, p.getSentAt() != null ? Timestamp.valueOf(p.getSentAt()) : null);
            stmt.setObject(7, p.getRespondedAt() != null ? Timestamp.valueOf(p.getRespondedAt()) : null);
            stmt.setTimestamp(8, Timestamp.valueOf(p.getUpdatedAt()));
            stmt.setLong(9, p.getId());
            stmt.executeUpdate();
            return p;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Proposal> findById(Long id) {
        String sql = "SELECT * FROM proposal WHERE id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Proposal p = mapRow(rs);
                    loadItems(p, conn);
                    return Optional.of(p);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Proposal> findAllWithFilters(ProposalStatus status, Long patientId, int page, int size) {
        return findFiltered(status, patientId, null, page, size);
    }

    @Override
    public long countByFilters(ProposalStatus status, Long patientId) {
        return countFiltered(status, patientId, null);
    }

    @Override
    public List<Proposal> findByPatientIdWithFilter(Long patientId, ProposalStatus status, int page, int size) {
        return findFiltered(status, patientId, patientId, page, size);
    }

    @Override
    public long countByPatientIdWithFilter(Long patientId, ProposalStatus status) {
        return countFiltered(status, patientId, patientId);
    }

    private List<Proposal> findFiltered(ProposalStatus status, Long patientId, Long forcePatientId, int page, int size) {
        var sb = new StringBuilder("SELECT * FROM proposal WHERE deleted_at IS NULL");
        var params = new ArrayList<>();
        if (status != null) { sb.append(" AND status = ?"); params.add(status.name()); }
        if (patientId != null) { sb.append(" AND patient_id = ?"); params.add(patientId); }
        sb.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (var rs = stmt.executeQuery()) {
                List<Proposal> proposals = new ArrayList<>();
                while (rs.next()) {
                    Proposal p = mapRow(rs);
                    loadItems(p, conn);
                    proposals.add(p);
                }
                return proposals;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private long countFiltered(ProposalStatus status, Long patientId, Long forcePatientId) {
        var sb = new StringBuilder("SELECT COUNT(*) FROM proposal WHERE deleted_at IS NULL");
        var params = new ArrayList<>();
        if (status != null) { sb.append(" AND status = ?"); params.add(status.name()); }
        if (patientId != null) { sb.append(" AND patient_id = ?"); params.add(patientId); }

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (var rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadItems(Proposal p, Connection conn) throws SQLException {
        String sql = "SELECT * FROM proposal_item WHERE proposal_id = ? ORDER BY id";
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, p.getId());
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ProposalItem item = ProposalItem.builder()
                            .category(ProposalItemCategory.valueOf(rs.getString("category")))
                            .name(rs.getString("name"))
                            .description(rs.getString("description"))
                            .unitPrice(rs.getBigDecimal("unit_price"))
                            .quantity(rs.getInt("quantity"))
                            .build();
                    item.setId(rs.getLong("id"));
                    p.addItem(item);
                }
            }
        }
    }

    private Proposal mapRow(ResultSet rs) throws SQLException {
        Proposal p = Proposal.builder()
                .patientId(rs.getLong("patient_id"))
                .title(rs.getString("title"))
                .currency(rs.getString("currency"))
                .discountRate(rs.getBigDecimal("discount_rate"))
                .validUntil(toLocalDateTime(rs.getTimestamp("valid_until")))
                .notes(rs.getString("notes"))
                .build();
        p.setId(rs.getLong("id"));
        p.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        p.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        p.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
        // Set fields via reflection for status, amounts, timestamps
        setField(p, "status", ProposalStatus.valueOf(rs.getString("status")));
        setField(p, "subtotal", rs.getBigDecimal("subtotal"));
        setField(p, "discountAmount", rs.getBigDecimal("discount_amount"));
        setField(p, "totalAmount", rs.getBigDecimal("total_amount"));
        setField(p, "sentAt", toLocalDateTime(rs.getTimestamp("sent_at")));
        setField(p, "respondedAt", toLocalDateTime(rs.getTimestamp("responded_at")));
        return p;
    }

    private void setField(Object obj, String name, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
