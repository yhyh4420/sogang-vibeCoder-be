package com.k.medtour.domain.aftercare.repository;

import com.k.medtour.domain.aftercare.entity.AftercareGuide;
import com.k.medtour.server.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public class JdbcAftercareGuideRepository implements AftercareGuideRepository {

    private final DataSource dataSource;

    public JdbcAftercareGuideRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public AftercareGuide save(AftercareGuide g) {
        if (g.getId() == null) return insert(g);
        return g;
    }

    private AftercareGuide insert(AftercareGuide g) {
        g.prePersist();
        String sql = """
            INSERT INTO aftercare_guide (journey_id, title, content, instructions, published_at, created_at, updated_at)
            VALUES (?, ?, ?, ?::jsonb, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, g.getJourneyId());
            stmt.setString(2, g.getTitle());
            stmt.setString(3, g.getContent());
            stmt.setString(4, g.getInstructions() != null ? JsonUtil.toJson(g.getInstructions()) : null);
            stmt.setTimestamp(5, g.getPublishedAt() != null ? Timestamp.valueOf(g.getPublishedAt()) : null);
            stmt.setTimestamp(6, Timestamp.valueOf(g.getCreatedAt()));
            stmt.setTimestamp(7, Timestamp.valueOf(g.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) { if (rs.next()) g.setId(rs.getLong(1)); }
            return g;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public Optional<AftercareGuide> findById(Long id) {
        return findOne("SELECT * FROM aftercare_guide WHERE id = ? AND deleted_at IS NULL", id);
    }

    @Override public Optional<AftercareGuide> findByJourneyIdAndDeletedAtIsNull(Long journeyId) {
        return findOne("SELECT * FROM aftercare_guide WHERE journey_id = ? AND deleted_at IS NULL", journeyId);
    }

    @Override public boolean existsByJourneyIdAndDeletedAtIsNull(Long journeyId) {
        String sql = "SELECT COUNT(*) FROM aftercare_guide WHERE journey_id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, journeyId);
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1) > 0; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Optional<AftercareGuide> findOne(String sql, Long param) {
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, param);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private AftercareGuide mapRow(ResultSet rs) throws SQLException {
        String instrJson = rs.getString("instructions");
        AftercareGuide g = AftercareGuide.builder()
                .journeyId(rs.getLong("journey_id"))
                .title(rs.getString("title"))
                .content(rs.getString("content"))
                .instructions(instrJson != null ? JsonUtil.fromJson(instrJson, new TypeReference<Map<String, Object>>() {}) : null)
                .build();
        g.setId(rs.getLong("id"));
        g.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        g.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        g.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
        return g;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) { return ts != null ? ts.toLocalDateTime() : null; }
}
