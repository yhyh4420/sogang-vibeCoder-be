package com.k.medtour.domain.chat.repository;

import com.k.medtour.domain.chat.entity.ChatRoom;
import com.k.medtour.domain.chat.enums.ChatRoomType;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository {

    ChatRoom save(ChatRoom chatRoom);

    Optional<ChatRoom> findById(String roomId);

    Optional<ChatRoom> findByIdWithParticipants(String roomId);

    boolean existsByRoomId(String roomId);

    List<ChatRoom> findAllByParticipantMemberId(Long memberId, int page, int size);

    List<ChatRoom> findAllByParticipantMemberIdAndType(Long memberId, ChatRoomType type, int page, int size);

    List<ChatRoom> findAllByType(ChatRoomType type, int page, int size);

    List<ChatRoom> findAllOrderByUpdatedAtDesc(int page, int size);

    List<ChatRoom> findAllByTypeOrderByUpdatedAtDesc(ChatRoomType type, int page, int size);

    long countAll();

    long countByType(ChatRoomType type);
}
