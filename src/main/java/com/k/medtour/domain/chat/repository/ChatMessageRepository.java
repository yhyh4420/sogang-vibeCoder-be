package com.k.medtour.domain.chat.repository;

import com.k.medtour.domain.chat.entity.ChatMessage;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository {

    ChatMessage save(ChatMessage message);

    Optional<ChatMessage> findById(String id);

    List<ChatMessage> findByRoomIdOrderBySentAtDesc(String roomId, int limit);

    List<ChatMessage> findByRoomIdAndCursorOrderBySentAtDesc(String roomId, String cursor, int limit);

    long countUnreadMessages(String roomId, String lastReadMessageId);

    long countByRoomId(String roomId);

    Optional<ChatMessage> findTopByChatRoom_RoomIdOrderBySentAtDesc(String roomId);

    long countUnreadMessages();
}
