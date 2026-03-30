package com.k.medtour.domain.journey.repository;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.journey.entity.JourneyScheduleItem;
import com.k.medtour.domain.journey.entity.StaffAssignment;
import com.k.medtour.domain.journey.enums.StaffAssignmentStatus;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcStaffAssignmentRepository implements StaffAssignmentRepository {

    private final DataSource dataSource;

    public JdbcStaffAssignmentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public StaffAssignment save(StaffAssignment a) {
        if (a.getId() == null) return insert(a);
        else return update(a);
    }

    private StaffAssignment insert(StaffAssignment a) {
        a.prePersist();
        String sql = """
            INSERT INTO staff_assignment (schedule_item_id, staff_id, status, assigned_at, completed_at,
                                          created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, a.getScheduleItem().getId());
            stmt.setLong(2, a.getStaff().getId());
            stmt.setString(3, a.getStatus().name());
            stmt.setObject(4, a.getAssignedAt() != null ? Timestamp.valueOf(a.getAssignedAt()) : null);
            stmt.setObject(5, a.getCompletedAt() != null ? Timestamp.valueOf(a.getCompletedAt()) : null);
            stmt.setTimestamp(6, Timestamp.valueOf(a.getCreatedAt()));
            stmt.setTimestamp(7, Timestamp.valueOf(a.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) { if (rs.next()) a.setId(rs.getLong(1)); }
            return a;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private StaffAssignment update(StaffAssignment a) {
        a.preUpdate();
        String sql = "UPDATE staff_assignment SET status = ?, completed_at = ?, updated_at = ? WHERE id = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, a.getStatus().name());
            stmt.setObject(2, a.getCompletedAt() != null ? Timestamp.valueOf(a.getCompletedAt()) : null);
            stmt.setTimestamp(3, Timestamp.valueOf(a.getUpdatedAt()));
            stmt.setLong(4, a.getId());
            stmt.executeUpdate();
            return a;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public Optional<StaffAssignment> findById(Long id) {
        String sql = "SELECT * FROM staff_assignment WHERE id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<StaffAssignment> findByScheduleItemId(Long scheduleItemId) {
        String sql = "SELECT * FROM staff_assignment WHERE schedule_item_id = ? AND deleted_at IS NULL";
        return findList(sql, scheduleItemId);
    }

    @Override
    public List<StaffAssignment> findByStaffIdAndDate(Long staffId, LocalDateTime startOfDay, LocalDateTime endOfDay) {
        String sql = """
            SELECT sa.* FROM staff_assignment sa
            JOIN journey_schedule_item jsi ON sa.schedule_item_id = jsi.id
            WHERE sa.staff_id = ? AND jsi.scheduled_at >= ? AND jsi.scheduled_at < ?
            AND sa.deleted_at IS NULL AND jsi.deleted_at IS NULL
            ORDER BY jsi.scheduled_at ASC
            """;
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, staffId);
            stmt.setTimestamp(2, Timestamp.valueOf(startOfDay));
            stmt.setTimestamp(3, Timestamp.valueOf(endOfDay));
            try (var rs = stmt.executeQuery()) {
                List<StaffAssignment> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public boolean existsByScheduleItemIdAndStaffId(Long scheduleItemId, Long staffId) {
        String sql = "SELECT COUNT(*) FROM staff_assignment WHERE schedule_item_id = ? AND staff_id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, scheduleItemId); stmt.setLong(2, staffId);
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1) > 0; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public Optional<StaffAssignment> findByScheduleItemIdAndStaffId(Long scheduleItemId, Long staffId) {
        String sql = "SELECT * FROM staff_assignment WHERE schedule_item_id = ? AND staff_id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, scheduleItemId); stmt.setLong(2, staffId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public boolean existsByJourneyIdAndStaffId(Long journeyId, Long staffId) {
        String sql = """
            SELECT COUNT(*) FROM staff_assignment sa
            JOIN journey_schedule_item jsi ON sa.schedule_item_id = jsi.id
            WHERE jsi.journey_id = ? AND sa.staff_id = ? AND sa.deleted_at IS NULL AND jsi.deleted_at IS NULL
            """;
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, journeyId); stmt.setLong(2, staffId);
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1) > 0; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private List<StaffAssignment> findList(String sql, Long param) {
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, param);
            try (var rs = stmt.executeQuery()) {
                List<StaffAssignment> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private StaffAssignment mapRow(ResultSet rs) throws SQLException {
        JourneyScheduleItem itemStub = JourneyScheduleItem.builder().build();
        itemStub.setId(rs.getLong("schedule_item_id"));
        Member staffStub = Member.builder().build();
        staffStub.setId(rs.getLong("staff_id"));

        StaffAssignment a = StaffAssignment.builder()
                .scheduleItem(itemStub)
                .staff(staffStub)
                .build();
        a.setId(rs.getLong("id"));
        a.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        a.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        setField(a, "status", StaffAssignmentStatus.valueOf(rs.getString("status")));
        setField(a, "assignedAt", toLocalDateTime(rs.getTimestamp("assigned_at")));
        setField(a, "completedAt", toLocalDateTime(rs.getTimestamp("completed_at")));
        return a;
    }

    private void setField(Object obj, String name, Object value) {
        try { var f = obj.getClass().getDeclaredField(name); f.setAccessible(true); f.set(obj, value); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) { return ts != null ? ts.toLocalDateTime() : null; }
}
