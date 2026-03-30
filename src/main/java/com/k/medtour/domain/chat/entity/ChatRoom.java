package com.k.medtour.domain.chat.entity;

import com.k.medtour.domain.chat.enums.ChatRoomType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    private String roomId;

    private ChatRoomType type;

    private Long journeyId;

    private String language;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<ChatRoomParticipant> participants = new ArrayList<>();

    private List<ChatMessage> messages = new ArrayList<>();

    @Builder
    public ChatRoom(String roomId, ChatRoomType type, Long journeyId, String language) {
        this.roomId = roomId;
        this.type = type;
        this.journeyId = journeyId;
        this.language = language;
    }

    public void addParticipant(ChatRoomParticipant participant) {
        this.participants.add(participant);
    }
}
