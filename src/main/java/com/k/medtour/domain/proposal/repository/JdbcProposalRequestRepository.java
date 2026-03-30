package com.k.medtour.domain.proposal.repository;

import com.k.medtour.domain.proposal.entity.ProposalRequest;
import com.k.medtour.domain.proposal.enums.ProposalRequestStatus;
import com.k.medtour.server.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcProposalRequestRepository implements ProposalRequestRepository {

    private final DataSource dataSource;

    public JdbcProposalRequestRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public ProposalRequest save(ProposalRequest req) {
        if (req.getId() == null) return insert(req);
        return req;
    }

    private ProposalRequest insert(ProposalRequest r) {
        r.prePersist();
        String sql = """
            INSERT INTO proposal_request (patient_id, desired_procedures, preferred_hospital_ids,
                                          arrival_date, departure_date, accommodation_preference,
                                          concierge_services, budget_min, budget_max, budget_currency,
                                          additional_requests, status, created_at, updated_at)
            VALUES (?, ?::jsonb, ?::jsonb, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, r.getPatientId());
            stmt.setString(2, r.getDesiredProcedures() != null ? JsonUtil.toJson(r.getDesiredProcedures()) : null);
            stmt.setString(3, r.getPreferredHospitalIds() != null ? JsonUtil.toJson(r.getPreferredHospitalIds()) : null);
            stmt.setObject(4, r.getArrivalDate());
            stmt.setObject(5, r.getDepartureDate());
            stmt.setString(6, r.getAccommodationPreference());
            stmt.setString(7, r.getConciergeServices() != null ? JsonUtil.toJson(r.getConciergeServices()) : null);
            stmt.setBigDecimal(8, r.getBudgetMin());
            stmt.setBigDecimal(9, r.getBudgetMax());
            stmt.setString(10, r.getBudgetCurrency());
            stmt.setString(11, r.getAdditionalRequests());
            stmt.setString(12, r.getStatus().name());
            stmt.setTimestamp(13, Timestamp.valueOf(r.getCreatedAt()));
            stmt.setTimestamp(14, Timestamp.valueOf(r.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) r.setId(rs.getLong(1));
            }
            return r;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<ProposalRequest> findById(Long id) {
        String sql = "SELECT * FROM proposal_request WHERE id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ProposalRequest> findByPatientId(Long patientId) {
        String sql = "SELECT * FROM proposal_request WHERE patient_id = ? AND deleted_at IS NULL ORDER BY created_at DESC";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, patientId);
            try (var rs = stmt.executeQuery()) {
                List<ProposalRequest> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ProposalRequest mapRow(ResultSet rs) throws SQLException {
        String procJson = rs.getString("desired_procedures");
        String hospJson = rs.getString("preferred_hospital_ids");
        String servJson = rs.getString("concierge_services");

        ProposalRequest r = ProposalRequest.builder()
                .patientId(rs.getLong("patient_id"))
                .desiredProcedures(procJson != null ? JsonUtil.fromJson(procJson, new TypeReference<List<String>>() {}) : null)
                .preferredHospitalIds(hospJson != null ? JsonUtil.fromJson(hospJson, new TypeReference<List<Long>>() {}) : null)
                .arrivalDate(rs.getObject("arrival_date", LocalDate.class))
                .departureDate(rs.getObject("departure_date", LocalDate.class))
                .accommodationPreference(rs.getString("accommodation_preference"))
                .conciergeServices(servJson != null ? JsonUtil.fromJson(servJson, new TypeReference<List<String>>() {}) : null)
                .budgetMin(rs.getBigDecimal("budget_min"))
                .budgetMax(rs.getBigDecimal("budget_max"))
                .budgetCurrency(rs.getString("budget_currency"))
                .additionalRequests(rs.getString("additional_requests"))
                .build();
        r.setId(rs.getLong("id"));
        r.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        r.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return r;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
