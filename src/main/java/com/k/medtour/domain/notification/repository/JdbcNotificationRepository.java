package com.k.medtour.domain.notification.repository;

import com.k.medtour.domain.notification.entity.Notification;
import com.k.medtour.domain.notification.enums.NotificationType;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcNotificationRepository implements NotificationRepository {

    private final DataSource dataSource;

    public JdbcNotificationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Notification save(Notification n) {
        if (n.getId() == null) return insert(n);
        else return update(n);
    }

    private Notification insert(Notification n) {
        n.prePersist();
        String sql = """
            INSERT INTO notification (member_id, type, title, content, reference_id, reference_type,
                                      is_read, read_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, n.getMemberId());
            stmt.setString(2, n.getType().name());
            stmt.setString(3, n.getTitle());
            stmt.setString(4, n.getContent());
            stmt.setObject(5, n.getReferenceId());
            stmt.setString(6, n.getReferenceType());
            stmt.setBoolean(7, n.getIsRead());
            stmt.setObject(8, n.getReadAt() != null ? Timestamp.valueOf(n.getReadAt()) : null);
            stmt.setTimestamp(9, Timestamp.valueOf(n.getCreatedAt()));
            stmt.setTimestamp(10, Timestamp.valueOf(n.getUpdatedAt()));
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) { if (rs.next()) n.setId(rs.getLong(1)); }
            return n;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Notification update(Notification n) {
        n.preUpdate();
        String sql = "UPDATE notification SET is_read = ?, read_at = ?, updated_at = ? WHERE id = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, n.getIsRead());
            stmt.setObject(2, n.getReadAt() != null ? Timestamp.valueOf(n.getReadAt()) : null);
            stmt.setTimestamp(3, Timestamp.valueOf(n.getUpdatedAt()));
            stmt.setLong(4, n.getId());
            stmt.executeUpdate();
            return n;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public Optional<Notification> findById(Long id) {
        String sql = "SELECT * FROM notification WHERE id = ? AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<Notification> findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long memberId, int page, int size) {
        String sql = "SELECT * FROM notification WHERE member_id = ? AND deleted_at IS NULL ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, memberId); stmt.setInt(2, size); stmt.setInt(3, page * size);
            try (var rs = stmt.executeQuery()) {
                List<Notification> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public long countByMemberIdAndDeletedAtIsNull(Long memberId) {
        return countSql("SELECT COUNT(*) FROM notification WHERE member_id = ? AND deleted_at IS NULL", memberId);
    }

    @Override public long countByMemberIdAndIsReadFalseAndDeletedAtIsNull(Long memberId) {
        return countSql("SELECT COUNT(*) FROM notification WHERE member_id = ? AND is_read = false AND deleted_at IS NULL", memberId);
    }

    @Override public int markAllAsReadByMemberId(Long memberId) {
        String sql = "UPDATE notification SET is_read = true, read_at = ?, updated_at = ? WHERE member_id = ? AND is_read = false AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            stmt.setTimestamp(1, now); stmt.setTimestamp(2, now); stmt.setLong(3, memberId);
            return stmt.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<Notification> findByTypeInOrderByCreatedAtDesc(List<NotificationType> types, int page, int size) {
        String placeholders = String.join(",", types.stream().map(t -> "?").toList());
        String sql = "SELECT * FROM notification WHERE type IN (" + placeholders + ") AND deleted_at IS NULL ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            for (NotificationType t : types) stmt.setString(idx++, t.name());
            stmt.setInt(idx++, size); stmt.setInt(idx, page * size);
            try (var rs = stmt.executeQuery()) {
                List<Notification> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public long countByTypeIn(List<NotificationType> types) {
        String placeholders = String.join(",", types.stream().map(t -> "?").toList());
        String sql = "SELECT COUNT(*) FROM notification WHERE type IN (" + placeholders + ") AND deleted_at IS NULL";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < types.size(); i++) stmt.setString(i + 1, types.get(i).name());
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private long countSql(String sql, Long param) {
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, param);
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = Notification.builder()
                .memberId(rs.getLong("member_id"))
                .type(NotificationType.valueOf(rs.getString("type")))
                .title(rs.getString("title"))
                .content(rs.getString("content"))
                .referenceId(rs.getObject("reference_id", Long.class))
                .referenceType(rs.getString("reference_type"))
                .build();
        n.setId(rs.getLong("id"));
        n.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        n.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        n.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
        if (rs.getBoolean("is_read")) n.markAsRead();
        return n;
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) { return ts != null ? ts.toLocalDateTime() : null; }
}
