package com.k.medtour.domain.journey.repository;

import com.k.medtour.domain.journey.entity.JourneyTemplate;
import com.k.medtour.domain.journey.enums.TemplateCategory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcJourneyTemplateRepository implements JourneyTemplateRepository {

    private final DataSource dataSource;

    public JdbcJourneyTemplateRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public JourneyTemplate save(JourneyTemplate t) {
        if (t.getId() == null) return insert(t);
        else return update(t);
    }

    private JourneyTemplate insert(JourneyTemplate t) {
        t.prePersist();
        String sql = "INSERT INTO journey_template (name, category, duration_days, usage_count, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, t.getName());
            stmt.setString(2, t.getCategory().name());
            stmt.setInt(3, t.getDurationDays());
            stmt.setInt(4, t.getUsageCount());
            stmt.setTimestamp(5, Timestamp.valueOf(t.getCreatedAt()));
            stmt.setTimestamp(6, Timestamp.valueOf(t.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) { if (rs.next()) t.setId(rs.getLong(1)); }
            return t;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private JourneyTemplate update(JourneyTemplate t) {
        t.preUpdate();
        String sql = "UPDATE journey_template SET name = ?, category = ?, duration_days = ?, usage_count = ?, updated_at = ?, deleted_at = ? WHERE id = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, t.getName());
            stmt.setString(2, t.getCategory().name());
            stmt.setInt(3, t.getDurationDays());
            stmt.setInt(4, t.getUsageCount());
            stmt.setTimestamp(5, Timestamp.valueOf(t.getUpdatedAt()));
            stmt.setObject(6, t.getDeletedAt() != null ? Timestamp.valueOf(t.getDeletedAt()) : null);
            stmt.setLong(7, t.getId());
            stmt.executeUpdate();
            return t;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public Optional<JourneyTemplate> findById(Long id) {
        return findOne("SELECT * FROM journey_template WHERE id = ?", id);
    }

    @Override public void deleteById(Long id) {
        String sql = "UPDATE journey_template SET deleted_at = ? WHERE id = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now())); stmt.setLong(2, id); stmt.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public boolean existsByName(String name) {
        return countBySql("SELECT COUNT(*) FROM journey_template WHERE name = ? AND deleted_at IS NULL", name) > 0;
    }

    @Override public boolean existsByNameAndIdNot(String name, Long id) {
        String sql = "SELECT COUNT(*) FROM journey_template WHERE name = ? AND id != ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name); stmt.setLong(2, id);
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1) > 0; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public Optional<JourneyTemplate> findByIdAndDeletedAtIsNull(Long id) {
        return findOne("SELECT * FROM journey_template WHERE id = ? AND deleted_at IS NULL", id);
    }

    @Override
    public List<JourneyTemplate> findAllByFilters(String keyword, TemplateCategory category, int page, int size) {
        var sb = new StringBuilder("SELECT * FROM journey_template WHERE deleted_at IS NULL");
        var params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) { sb.append(" AND name ILIKE ?"); params.add("%" + keyword + "%"); }
        if (category != null) { sb.append(" AND category = ?"); params.add(category.name()); }
        sb.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(size); params.add(page * size);
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
            try (var rs = stmt.executeQuery()) {
                List<JourneyTemplate> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public long countByFilters(String keyword, TemplateCategory category) {
        var sb = new StringBuilder("SELECT COUNT(*) FROM journey_template WHERE deleted_at IS NULL");
        var params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) { sb.append(" AND name ILIKE ?"); params.add("%" + keyword + "%"); }
        if (category != null) { sb.append(" AND category = ?"); params.add(category.name()); }
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Optional<JourneyTemplate> findOne(String sql, Long id) {
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private long countBySql(String sql, String param) {
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, param);
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private JourneyTemplate mapRow(ResultSet rs) throws SQLException {
        JourneyTemplate t = JourneyTemplate.builder()
                .name(rs.getString("name"))
                .category(TemplateCategory.valueOf(rs.getString("category")))
                .durationDays(rs.getInt("duration_days"))
                .build();
        t.setId(rs.getLong("id"));
        t.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        t.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        t.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
        try { var f = JourneyTemplate.class.getDeclaredField("usageCount"); f.setAccessible(true); f.set(t, rs.getInt("usage_count")); }
        catch (Exception e) { throw new RuntimeException(e); }
        return t;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) { return ts != null ? ts.toLocalDateTime() : null; }
}
