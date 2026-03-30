package com.k.medtour.domain.journey.repository;

import com.k.medtour.domain.journey.entity.Journey;
import com.k.medtour.domain.journey.entity.JourneyScheduleItem;
import com.k.medtour.domain.journey.enums.ScheduleItemStatus;
import com.k.medtour.domain.journey.enums.ScheduleItemType;
import com.k.medtour.server.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcJourneyScheduleItemRepository implements JourneyScheduleItemRepository {

    private final DataSource dataSource;

    public JdbcJourneyScheduleItemRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public JourneyScheduleItem save(JourneyScheduleItem item) {
        if (item.getId() == null) return insert(item);
        else return update(item);
    }

    private JourneyScheduleItem insert(JourneyScheduleItem item) {
        item.prePersist();
        String sql = """
            INSERT INTO journey_schedule_item (journey_id, day_number, scheduled_at, title, type,
                                               description, status, duration_minutes, location,
                                               completed_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, item.getJourney().getId());
            stmt.setInt(2, item.getDayNumber());
            stmt.setTimestamp(3, Timestamp.valueOf(item.getScheduledAt()));
            stmt.setString(4, item.getTitle());
            stmt.setString(5, item.getType().name());
            stmt.setString(6, item.getDescription());
            stmt.setString(7, item.getStatus().name());
            stmt.setObject(8, item.getDurationMinutes());
            stmt.setString(9, item.getLocation() != null ? JsonUtil.toJson(item.getLocation()) : null);
            stmt.setObject(10, item.getCompletedAt() != null ? Timestamp.valueOf(item.getCompletedAt()) : null);
            stmt.setTimestamp(11, Timestamp.valueOf(item.getCreatedAt()));
            stmt.setTimestamp(12, Timestamp.valueOf(item.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) item.setId(rs.getLong(1));
            }
            return item;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private JourneyScheduleItem update(JourneyScheduleItem item) {
        item.preUpdate();
        String sql = """
            UPDATE journey_schedule_item SET scheduled_at = ?, title = ?, description = ?,
                                            status = ?, duration_minutes = ?, location = ?::jsonb,
                                            completed_at = ?, updated_at = ?, deleted_at = ?
            WHERE id = ?
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(item.getScheduledAt()));
            stmt.setString(2, item.getTitle());
            stmt.setString(3, item.getDescription());
            stmt.setString(4, item.getStatus().name());
            stmt.setObject(5, item.getDurationMinutes());
            stmt.setString(6, item.getLocation() != null ? JsonUtil.toJson(item.getLocation()) : null);
            stmt.setObject(7, item.getCompletedAt() != null ? Timestamp.valueOf(item.getCompletedAt()) : null);
            stmt.setTimestamp(8, Timestamp.valueOf(item.getUpdatedAt()));
            stmt.setObject(9, item.getDeletedAt() != null ? Timestamp.valueOf(item.getDeletedAt()) : null);
            stmt.setLong(10, item.getId());
            stmt.executeUpdate();
            return item;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<JourneyScheduleItem> findById(Long id) {
        String sql = "SELECT * FROM journey_schedule_item WHERE id = ?";
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
    public void deleteById(Long id) {
        String sql = "UPDATE journey_schedule_item SET deleted_at = ? WHERE id = ?";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<JourneyScheduleItem> findByJourneyIdOrderByScheduledAtAsc(Long journeyId) {
        String sql = "SELECT * FROM journey_schedule_item WHERE journey_id = ? AND deleted_at IS NULL ORDER BY scheduled_at ASC";
        return findListByJourneyId(sql, journeyId);
    }

    @Override
    public List<JourneyScheduleItem> findByJourneyIdAndDate(Long journeyId, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        String sql = "SELECT * FROM journey_schedule_item WHERE journey_id = ? AND scheduled_at >= ? AND scheduled_at < ? AND deleted_at IS NULL ORDER BY scheduled_at ASC";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, journeyId);
            stmt.setTimestamp(2, Timestamp.valueOf(startOfDay));
            stmt.setTimestamp(3, Timestamp.valueOf(endOfDay));
            try (var rs = stmt.executeQuery()) {
                List<JourneyScheduleItem> items = new ArrayList<>();
                while (rs.next()) items.add(mapRow(rs));
                return items;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<JourneyScheduleItem> findByIdAndJourneyId(Long id, Long journeyId) {
        String sql = "SELECT * FROM journey_schedule_item WHERE id = ? AND journey_id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.setLong(2, journeyId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long countByScheduledAtBetweenAndDeletedAtIsNull(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT COUNT(*) FROM journey_schedule_item WHERE scheduled_at >= ? AND scheduled_at <= ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(start));
            stmt.setTimestamp(2, Timestamp.valueOf(end));
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1); }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<JourneyScheduleItem> findListByJourneyId(String sql, Long journeyId) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, journeyId);
            try (var rs = stmt.executeQuery()) {
                List<JourneyScheduleItem> items = new ArrayList<>();
                while (rs.next()) items.add(mapRow(rs));
                return items;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private JourneyScheduleItem mapRow(ResultSet rs) throws SQLException {
        Journey journeyStub = Journey.builder().build();
        journeyStub.setId(rs.getLong("journey_id"));

        String locationJson = rs.getString("location");
        Map<String, Object> location = locationJson != null
                ? JsonUtil.fromJson(locationJson, new TypeReference<Map<String, Object>>() {})
                : null;

        JourneyScheduleItem item = JourneyScheduleItem.builder()
                .journey(journeyStub)
                .dayNumber(rs.getInt("day_number"))
                .scheduledAt(rs.getTimestamp("scheduled_at").toLocalDateTime())
                .title(rs.getString("title"))
                .type(ScheduleItemType.valueOf(rs.getString("type")))
                .description(rs.getString("description"))
                .durationMinutes(rs.getObject("duration_minutes", Integer.class))
                .location(location)
                .build();
        item.setId(rs.getLong("id"));
        item.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        item.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        item.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
        setField(item, "status", ScheduleItemStatus.valueOf(rs.getString("status")));
        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) setField(item, "completedAt", completedAt.toLocalDateTime());
        return item;
    }

    private void setField(Object obj, String name, Object value) {
        try { var f = obj.getClass().getDeclaredField(name); f.setAccessible(true); f.set(obj, value); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) { return ts != null ? ts.toLocalDateTime() : null; }
}
