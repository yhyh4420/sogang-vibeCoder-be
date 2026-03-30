package com.k.medtour.domain.chat.repository;

import com.k.medtour.domain.chat.entity.ChatRoom;
import com.k.medtour.domain.chat.entity.ChatRoomParticipant;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcChatRoomParticipantRepository implements ChatRoomParticipantRepository {

    private final DataSource dataSource;

    public JdbcChatRoomParticipantRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public ChatRoomParticipant save(ChatRoomParticipant p) {
        if (p.getId() == null) {
            String sql = "INSERT INTO chat_room_participant (room_id, member_id, last_read_message_id, joined_at) VALUES (?, ?, ?, ?)";
            try (var conn = dataSource.getConnection();
                 var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, p.getChatRoom().getRoomId());
                stmt.setLong(2, p.getMemberId());
                stmt.setString(3, p.getLastReadMessageId());
                stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                stmt.executeUpdate();
                try (var rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) setField(p, "id", rs.getLong(1));
                }
                return p;
            } catch (SQLException e) { throw new RuntimeException(e); }
        } else {
            String sql = "UPDATE chat_room_participant SET last_read_message_id = ? WHERE id = ?";
            try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, p.getLastReadMessageId());
                stmt.setLong(2, p.getId());
                stmt.executeUpdate();
                return p;
            } catch (SQLException e) { throw new RuntimeException(e); }
        }
    }

    @Override
    public boolean existsByChatRoom_RoomIdAndMemberId(String roomId, Long memberId) {
        String sql = "SELECT COUNT(*) FROM chat_room_participant WHERE room_id = ? AND member_id = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomId); stmt.setLong(2, memberId);
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1) > 0; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public Optional<ChatRoomParticipant> findByChatRoom_RoomIdAndMemberId(String roomId, Long memberId) {
        String sql = "SELECT * FROM chat_room_participant WHERE room_id = ? AND member_id = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomId); stmt.setLong(2, memberId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<ChatRoomParticipant> findAllByChatRoom_RoomId(String roomId) {
        String sql = "SELECT * FROM chat_room_participant WHERE room_id = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomId);
            try (var rs = stmt.executeQuery()) {
                List<ChatRoomParticipant> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<Long> findMemberIdsByRoomId(String roomId) {
        String sql = "SELECT member_id FROM chat_room_participant WHERE room_id = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomId);
            try (var rs = stmt.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (rs.next()) ids.add(rs.getLong("member_id"));
                return ids;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private ChatRoomParticipant mapRow(ResultSet rs) throws SQLException {
        ChatRoom roomStub = ChatRoom.builder().roomId(rs.getString("room_id")).build();
        ChatRoomParticipant p = ChatRoomParticipant.builder()
                .chatRoom(roomStub)
                .memberId(rs.getLong("member_id"))
                .build();
        setField(p, "id", rs.getLong("id"));
        setField(p, "lastReadMessageId", rs.getString("last_read_message_id"));
        setField(p, "joinedAt", rs.getTimestamp("joined_at") != null ? rs.getTimestamp("joined_at").toLocalDateTime() : null);
        return p;
    }

    private void setField(Object obj, String name, Object value) {
        try { var f = obj.getClass().getDeclaredField(name); f.setAccessible(true); f.set(obj, value); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
