package com.k.medtour.domain.journey.repository;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.journey.entity.Journey;
import com.k.medtour.domain.journey.enums.JourneyStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcJourneyRepository implements JourneyRepository {

    private final DataSource dataSource;

    public JdbcJourneyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Journey save(Journey journey) {
        if (journey.getId() == null) return insert(journey);
        else return update(journey);
    }

    private Journey insert(Journey j) {
        j.prePersist();
        String sql = """
            INSERT INTO journey (patient_id, title, status, start_date, end_date, notes, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, j.getPatient().getId());
            stmt.setString(2, j.getTitle());
            stmt.setString(3, j.getStatus().name());
            stmt.setObject(4, j.getStartDate());
            stmt.setObject(5, j.getEndDate());
            stmt.setString(6, j.getNotes());
            stmt.setTimestamp(7, Timestamp.valueOf(j.getCreatedAt()));
            stmt.setTimestamp(8, Timestamp.valueOf(j.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) j.setId(rs.getLong(1));
            }
            return j;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Journey update(Journey j) {
        j.preUpdate();
        String sql = "UPDATE journey SET status = ?, updated_at = ?, deleted_at = ? WHERE id = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, j.getStatus().name());
            stmt.setTimestamp(2, Timestamp.valueOf(j.getUpdatedAt()));
            stmt.setObject(3, j.getDeletedAt() != null ? Timestamp.valueOf(j.getDeletedAt()) : null);
            stmt.setLong(4, j.getId());
            stmt.executeUpdate();
            return j;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Journey> findById(Long id) {
        return findOne("SELECT * FROM journey WHERE id = ?", id);
    }

    @Override
    public Optional<Journey> findByIdAndDeletedAtIsNull(Long id) {
        return findOne("SELECT * FROM journey WHERE id = ? AND deleted_at IS NULL", id);
    }

    private Optional<Journey> findOne(String sql, Long id) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs, conn));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Journey> findAllByFilters(JourneyStatus status, Long patientId,
                                           LocalDate startDateFrom, LocalDate startDateTo,
                                           int page, int size) {
        var sb = new StringBuilder("SELECT * FROM journey WHERE deleted_at IS NULL");
        var params = new ArrayList<>();
        if (status != null) { sb.append(" AND status = ?"); params.add(status.name()); }
        if (patientId != null) { sb.append(" AND patient_id = ?"); params.add(patientId); }
        if (startDateFrom != null) { sb.append(" AND start_date >= ?"); params.add(startDateFrom); }
        if (startDateTo != null) { sb.append(" AND start_date <= ?"); params.add(startDateTo); }
        sb.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(size); params.add(page * size);

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
            try (var rs = stmt.executeQuery()) {
                List<Journey> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs, conn));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long countByFilters(JourneyStatus status, Long patientId,
                               LocalDate startDateFrom, LocalDate startDateTo) {
        var sb = new StringBuilder("SELECT COUNT(*) FROM journey WHERE deleted_at IS NULL");
        var params = new ArrayList<>();
        if (status != null) { sb.append(" AND status = ?"); params.add(status.name()); }
        if (patientId != null) { sb.append(" AND patient_id = ?"); params.add(patientId); }
        if (startDateFrom != null) { sb.append(" AND start_date >= ?"); params.add(startDateFrom); }
        if (startDateTo != null) { sb.append(" AND start_date <= ?"); params.add(startDateTo); }

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1); }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean existsByPatientIdAndStatusIn(Long patientId, List<JourneyStatus> statuses) {
        String placeholders = String.join(",", statuses.stream().map(s -> "?").toList());
        String sql = "SELECT COUNT(*) FROM journey WHERE patient_id = ? AND status IN (" + placeholders + ") AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, patientId);
            for (int i = 0; i < statuses.size(); i++) stmt.setString(i + 2, statuses.get(i).name());
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1) > 0; }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Journey> findByPatientIdAndStatusIn(Long patientId, List<JourneyStatus> statuses) {
        String placeholders = String.join(",", statuses.stream().map(s -> "?").toList());
        String sql = "SELECT * FROM journey WHERE patient_id = ? AND status IN (" + placeholders + ") AND deleted_at IS NULL ORDER BY start_date";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, patientId);
            for (int i = 0; i < statuses.size(); i++) stmt.setString(i + 2, statuses.get(i).name());
            try (var rs = stmt.executeQuery()) {
                List<Journey> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs, conn));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Journey mapRow(ResultSet rs, Connection conn) throws SQLException {
        Member patientStub = Member.builder().build();
        patientStub.setId(rs.getLong("patient_id"));
        // Load patient name
        try (var pstmt = conn.prepareStatement("SELECT name, language, phone FROM member WHERE id = ?")) {
            pstmt.setLong(1, patientStub.getId());
            try (var prs = pstmt.executeQuery()) {
                if (prs.next()) {
                    patientStub = Member.builder().name(prs.getString("name"))
                            .language(prs.getString("language")).phone(prs.getString("phone")).build();
                    patientStub.setId(rs.getLong("patient_id"));
                }
            }
        }

        Journey j = Journey.builder()
                .patient(patientStub)
                .title(rs.getString("title"))
                .startDate(rs.getObject("start_date", LocalDate.class))
                .endDate(rs.getObject("end_date", LocalDate.class))
                .notes(rs.getString("notes"))
                .build();
        j.setId(rs.getLong("id"));
        j.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        j.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        j.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
        setField(j, "status", JourneyStatus.valueOf(rs.getString("status")));
        return j;
    }

    private void setField(Object obj, String name, Object value) {
        try { var f = obj.getClass().getDeclaredField(name); f.setAccessible(true); f.set(obj, value); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) { return ts != null ? ts.toLocalDateTime() : null; }
}
