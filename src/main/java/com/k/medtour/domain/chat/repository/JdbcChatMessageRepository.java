package com.k.medtour.domain.chat.repository;

import com.k.medtour.domain.chat.entity.ChatMessage;
import com.k.medtour.domain.chat.entity.ChatRoom;
import com.k.medtour.domain.chat.enums.ChatMessageType;
import com.k.medtour.server.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcChatMessageRepository implements ChatMessageRepository {

    private final DataSource dataSource;

    public JdbcChatMessageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public ChatMessage save(ChatMessage msg) {
        String sql = """
            INSERT INTO chat_message (id, room_id, sender_id, sender_name, sender_role, type, content,
                                      translated_content, file_id, caption, is_secure, sent_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
            """;
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, msg.getId());
            stmt.setString(2, msg.getChatRoom().getRoomId());
            stmt.setLong(3, msg.getSenderId());
            stmt.setString(4, msg.getSenderName());
            stmt.setString(5, msg.getSenderRole());
            stmt.setString(6, msg.getType().name());
            stmt.setString(7, msg.getContent());
            stmt.setString(8, msg.getTranslatedContent() != null && !msg.getTranslatedContent().isEmpty()
                    ? JsonUtil.toJson(msg.getTranslatedContent()) : null);
            stmt.setObject(9, msg.getFileId());
            stmt.setString(10, msg.getCaption());
            stmt.setObject(11, msg.getIsSecure());
            stmt.setTimestamp(12, Timestamp.valueOf(msg.getSentAt()));
            stmt.executeUpdate();
            return msg;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public Optional<ChatMessage> findById(String id) {
        String sql = "SELECT * FROM chat_message WHERE id = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<ChatMessage> findByRoomIdOrderBySentAtDesc(String roomId, int limit) {
        String sql = "SELECT * FROM chat_message WHERE room_id = ? ORDER BY sent_at DESC LIMIT ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomId); stmt.setInt(2, limit);
            try (var rs = stmt.executeQuery()) {
                List<ChatMessage> msgs = new ArrayList<>();
                while (rs.next()) msgs.add(mapRow(rs));
                return msgs;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<ChatMessage> findByRoomIdAndCursorOrderBySentAtDesc(String roomId, String cursor, int limit) {
        String sql = """
            SELECT * FROM chat_message WHERE room_id = ? AND sent_at < (SELECT sent_at FROM chat_message WHERE id = ?)
            ORDER BY sent_at DESC LIMIT ?
            """;
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomId); stmt.setString(2, cursor); stmt.setInt(3, limit);
            try (var rs = stmt.executeQuery()) {
                List<ChatMessage> msgs = new ArrayList<>();
                while (rs.next()) msgs.add(mapRow(rs));
                return msgs;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public long countUnreadMessages(String roomId, String lastReadMessageId) {
        if (lastReadMessageId == null) return countByRoomId(roomId);
        String sql = """
            SELECT COUNT(*) FROM chat_message WHERE room_id = ?
            AND sent_at > (SELECT sent_at FROM chat_message WHERE id = ?)
            """;
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomId); stmt.setString(2, lastReadMessageId);
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public long countByRoomId(String roomId) {
        String sql = "SELECT COUNT(*) FROM chat_message WHERE room_id = ?";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomId);
            try (var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public Optional<ChatMessage> findTopByChatRoom_RoomIdOrderBySentAtDesc(String roomId) {
        String sql = "SELECT * FROM chat_message WHERE room_id = ? ORDER BY sent_at DESC LIMIT 1";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
                return Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public long countUnreadMessages() {
        // Simplified: count all messages that are not read by any admin
        String sql = "SELECT COUNT(*) FROM chat_message";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) { rs.next(); return rs.getLong(1); }
        catch (SQLException e) { throw new RuntimeException(e); }
    }

    private ChatMessage mapRow(ResultSet rs) throws SQLException {
        ChatRoom roomStub = ChatRoom.builder().roomId(rs.getString("room_id")).build();
        String translatedJson = rs.getString("translated_content");
        Map<String, String> translated = translatedJson != null
                ? JsonUtil.fromJson(translatedJson, new TypeReference<Map<String, String>>() {})
                : null;

        return ChatMessage.builder()
                .id(rs.getString("id"))
                .chatRoom(roomStub)
                .senderId(rs.getLong("sender_id"))
                .senderName(rs.getString("sender_name"))
                .senderRole(rs.getString("sender_role"))
                .type(ChatMessageType.valueOf(rs.getString("type")))
                .content(rs.getString("content"))
                .translatedContent(translated)
                .fileId(rs.getObject("file_id", Long.class))
                .caption(rs.getString("caption"))
                .isSecure(rs.getObject("is_secure", Boolean.class))
                .sentAt(rs.getTimestamp("sent_at").toLocalDateTime())
                .build();
    }
}
