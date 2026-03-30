package com.k.medtour.domain.chat.repository;

import com.k.medtour.domain.chat.entity.ChatRoom;
import com.k.medtour.domain.chat.enums.ChatRoomType;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcChatRoomRepository implements ChatRoomRepository {

    private final DataSource dataSource;

    public JdbcChatRoomRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public ChatRoom save(ChatRoom room) {
        LocalDateTime now = LocalDateTime.now();
        String sql = """
            INSERT INTO chat_room (room_id, type, journey_id, language, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (room_id) DO UPDATE SET updated_at = ?
            """;
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, room.getRoomId());
            stmt.setString(2, room.getType().name());
            stmt.setObject(3, room.getJourneyId());
            stmt.setString(4, room.getLanguage());
            stmt.setTimestamp(5, Timestamp.valueOf(now));
            stmt.setTimestamp(6, Timestamp.valueOf(now));
            stmt.setTimestamp(7, Timestamp.valueOf(now));
            stmt.executeUpdate();
            setField(room, "createdAt", now);
            setField(room, "updatedAt", now);
            return room;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public Optional<ChatRoom> findById(String roomId) {
        return findOne("SELECT * FROM chat_room WHERE room_id = ?", roomId);
    }

    @Override public Optional<ChatRoom> findByIdWithParticipants(String roomId) {
        return findById(roomId);
    }

    @Override public boolean existsByRoomId(String roomId) {
        String sql = "SELECT COUNT(*) FROM chat_room WHERE room_id = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomId);
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1) > 0; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<ChatRoom> findAllByParticipantMemberId(Long memberId, int page, int size) {
        String sql = """
            SELECT cr.* FROM chat_room cr
            JOIN chat_room_participant crp ON cr.room_id = crp.room_id
            WHERE crp.member_id = ? ORDER BY cr.updated_at DESC LIMIT ? OFFSET ?
            """;
        return findRoomList(sql, memberId, page, size);
    }

    @Override
    public List<ChatRoom> findAllByParticipantMemberIdAndType(Long memberId, ChatRoomType type, int page, int size) {
        String sql = """
            SELECT cr.* FROM chat_room cr
            JOIN chat_room_participant crp ON cr.room_id = crp.room_id
            WHERE crp.member_id = ? AND cr.type = ? ORDER BY cr.updated_at DESC LIMIT ? OFFSET ?
            """;
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, memberId); stmt.setString(2, type.name());
            stmt.setInt(3, size); stmt.setInt(4, page * size);
            return extractRoomList(stmt);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public List<ChatRoom> findAllByType(ChatRoomType type, int page, int size) {
        String sql = "SELECT * FROM chat_room WHERE type = ? ORDER BY updated_at DESC LIMIT ? OFFSET ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type.name()); stmt.setInt(2, size); stmt.setInt(3, page * size);
            return extractRoomList(stmt);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public List<ChatRoom> findAllOrderByUpdatedAtDesc(int page, int size) {
        String sql = "SELECT * FROM chat_room ORDER BY updated_at DESC LIMIT ? OFFSET ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, size); stmt.setInt(2, page * size);
            return extractRoomList(stmt);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public List<ChatRoom> findAllByTypeOrderByUpdatedAtDesc(ChatRoomType type, int page, int size) {
        return findAllByType(type, page, size);
    }

    @Override public long countAll() {
        return countSql("SELECT COUNT(*) FROM chat_room");
    }

    @Override public long countByType(ChatRoomType type) {
        String sql = "SELECT COUNT(*) FROM chat_room WHERE type = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type.name());
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Optional<ChatRoom> findOne(String sql, String roomId) {
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private List<ChatRoom> findRoomList(String sql, Long memberId, int page, int size) {
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, memberId); stmt.setInt(2, size); stmt.setInt(3, page * size);
            return extractRoomList(stmt);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private List<ChatRoom> extractRoomList(PreparedStatement stmt) throws SQLException {
        try (var rs = stmt.executeQuery()) {
            List<ChatRoom> rooms = new ArrayList<>();
            while (rs.next()) rooms.add(mapRow(rs));
            return rooms;
        }
    }

    private long countSql(String sql) {
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1); }
        catch (SQLException e) { throw new RuntimeException(e); }
    }

    private ChatRoom mapRow(ResultSet rs) throws SQLException {
        ChatRoom room = ChatRoom.builder()
                .roomId(rs.getString("room_id"))
                .type(ChatRoomType.valueOf(rs.getString("type")))
                .journeyId(rs.getObject("journey_id", Long.class))
                .language(rs.getString("language"))
                .build();
        setField(room, "createdAt", toLocalDateTime(rs.getTimestamp("created_at")));
        setField(room, "updatedAt", toLocalDateTime(rs.getTimestamp("updated_at")));
        return room;
    }

    private void setField(Object obj, String name, Object value) {
        try { var f = obj.getClass().getDeclaredField(name); f.setAccessible(true); f.set(obj, value); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) { return ts != null ? ts.toLocalDateTime() : null; }
}
