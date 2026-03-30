package com.k.medtour.domain.aftercare.repository;

import com.k.medtour.domain.aftercare.entity.StaffReport;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class JdbcStaffReportRepository implements StaffReportRepository {

    private final DataSource dataSource;

    public JdbcStaffReportRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public StaffReport save(StaffReport r) {
        if (r.getId() == null) return insert(r);
        return r;
    }

    private StaffReport insert(StaffReport r) {
        r.prePersist();
        String sql = """
            INSERT INTO staff_report (journey_id, staff_id, report_content, work_hours, completed_at,
                                      created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, r.getJourneyId());
            stmt.setLong(2, r.getStaffId());
            stmt.setString(3, r.getReportContent());
            stmt.setObject(4, r.getWorkHours());
            stmt.setObject(5, r.getCompletedAt() != null ? Timestamp.valueOf(r.getCompletedAt()) : null);
            stmt.setTimestamp(6, Timestamp.valueOf(r.getCreatedAt()));
            stmt.setTimestamp(7, Timestamp.valueOf(r.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) { if (rs.next()) r.setId(rs.getLong(1)); }
            return r;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public Optional<StaffReport> findById(Long id) {
        String sql = "SELECT * FROM staff_report WHERE id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public boolean existsByJourneyIdAndStaffIdAndDeletedAtIsNull(Long journeyId, Long staffId) {
        String sql = "SELECT COUNT(*) FROM staff_report WHERE journey_id = ? AND staff_id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, journeyId); stmt.setLong(2, staffId);
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1) > 0; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public Optional<StaffReport> findByJourneyIdAndStaffIdAndDeletedAtIsNull(Long journeyId, Long staffId) {
        String sql = "SELECT * FROM staff_report WHERE journey_id = ? AND staff_id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, journeyId); stmt.setLong(2, staffId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private StaffReport mapRow(ResultSet rs) throws SQLException {
        StaffReport r = StaffReport.builder()
                .journeyId(rs.getLong("journey_id"))
                .staffId(rs.getLong("staff_id"))
                .reportContent(rs.getString("report_content"))
                .workHours(rs.getObject("work_hours", Double.class))
                .build();
        r.setId(rs.getLong("id"));
        r.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        r.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        r.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
        return r;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) { return ts != null ? ts.toLocalDateTime() : null; }
}
