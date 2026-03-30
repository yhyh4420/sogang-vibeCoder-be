package com.k.medtour.domain.chat.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomParticipant {

    private Long id;

    private ChatRoom chatRoom;

    private Long memberId;

    private String lastReadMessageId;

    private LocalDateTime joinedAt;

    @Builder
    public ChatRoomParticipant(ChatRoom chatRoom, Long memberId) {
        this.chatRoom = chatRoom;
        this.memberId = memberId;
    }

    public void updateLastReadMessageId(String messageId) {
        this.lastReadMessageId = messageId;
    }
}
