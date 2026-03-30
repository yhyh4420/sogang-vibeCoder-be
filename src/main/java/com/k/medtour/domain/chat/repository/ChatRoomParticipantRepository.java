package com.k.medtour.domain.chat.repository;

import com.k.medtour.domain.chat.entity.ChatRoomParticipant;

import java.util.List;
import java.util.Optional;

public interface ChatRoomParticipantRepository {

    ChatRoomParticipant save(ChatRoomParticipant participant);

    boolean existsByChatRoom_RoomIdAndMemberId(String roomId, Long memberId);

    Optional<ChatRoomParticipant> findByChatRoom_RoomIdAndMemberId(String roomId, Long memberId);

    List<ChatRoomParticipant> findAllByChatRoom_RoomId(String roomId);

    List<Long> findMemberIdsByRoomId(String roomId);
}
