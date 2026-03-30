package com.k.medtour.domain.journey.repository;

import com.k.medtour.domain.journey.entity.JourneyTemplateItem;
import com.k.medtour.domain.journey.enums.ScheduleItemType;
import com.k.medtour.server.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JdbcJourneyTemplateItemRepository implements JourneyTemplateItemRepository {

    private final DataSource dataSource;

    public JdbcJourneyTemplateItemRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public JourneyTemplateItem save(JourneyTemplateItem item) {
        if (item.getId() == null) return insert(item);
        return item;
    }

    private JourneyTemplateItem insert(JourneyTemplateItem item) {
        item.prePersist();
        String sql = """
            INSERT INTO journey_template_item (template_id, day_offset, time_offset, title, type,
                                               description, duration_minutes, location, required_staff,
                                               sort_order, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, item.getTemplate().getId());
            stmt.setInt(2, item.getDayOffset());
            stmt.setString(3, item.getTimeOffset());
            stmt.setString(4, item.getTitle());
            stmt.setString(5, item.getType().name());
            stmt.setString(6, item.getDescription());
            stmt.setObject(7, item.getDurationMinutes());
            stmt.setString(8, item.getLocation() != null ? JsonUtil.toJson(item.getLocation()) : null);
            stmt.setString(9, item.getRequiredStaff() != null ? JsonUtil.toJson(item.getRequiredStaff()) : null);
            stmt.setInt(10, item.getSortOrder());
            stmt.setTimestamp(11, Timestamp.valueOf(item.getCreatedAt()));
            stmt.setTimestamp(12, Timestamp.valueOf(item.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) { if (rs.next()) item.setId(rs.getLong(1)); }
            return item;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<JourneyTemplateItem> findByTemplateIdOrderBySortOrderAsc(Long templateId) {
        String sql = "SELECT * FROM journey_template_item WHERE template_id = ? AND deleted_at IS NULL ORDER BY sort_order ASC";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, templateId);
            try (var rs = stmt.executeQuery()) {
                List<JourneyTemplateItem> items = new ArrayList<>();
                while (rs.next()) items.add(mapRow(rs));
                return items;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void deleteByTemplateId(Long templateId) {
        String sql = "UPDATE journey_template_item SET deleted_at = ? WHERE template_id = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(2, templateId);
            stmt.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private JourneyTemplateItem mapRow(ResultSet rs) throws SQLException {
        String locJson = rs.getString("location");
        String staffJson = rs.getString("required_staff");

        JourneyTemplateItem item = JourneyTemplateItem.builder()
                .dayOffset(rs.getInt("day_offset"))
                .timeOffset(rs.getString("time_offset"))
                .title(rs.getString("title"))
                .type(ScheduleItemType.valueOf(rs.getString("type")))
                .description(rs.getString("description"))
                .durationMinutes(rs.getObject("duration_minutes", Integer.class))
                .location(locJson != null ? JsonUtil.fromJson(locJson, new TypeReference<Map<String, Object>>() {}) : null)
                .requiredStaff(staffJson != null ? JsonUtil.fromJson(staffJson, new TypeReference<List<String>>() {}) : null)
                .sortOrder(rs.getInt("sort_order"))
                .build();
        item.setId(rs.getLong("id"));
        item.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        item.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return item;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) { return ts != null ? ts.toLocalDateTime() : null; }
}
