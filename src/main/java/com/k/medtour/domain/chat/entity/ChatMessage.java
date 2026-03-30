package com.k.medtour.domain.chat.entity;

import com.k.medtour.domain.chat.enums.ChatMessageType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    private String id;

    private ChatRoom chatRoom;

    private Long senderId;

    private String senderName;

    private String senderRole;

    private ChatMessageType type;

    private String content;

    private Map<String, String> translatedContent = new HashMap<>();

    private Long fileId;

    private String caption;

    private Boolean isSecure;

    private LocalDateTime sentAt;

    @Builder
    public ChatMessage(String id, ChatRoom chatRoom, Long senderId, String senderName,
                       String senderRole, ChatMessageType type, String content,
                       Map<String, String> translatedContent, Long fileId,
                       String caption, Boolean isSecure, LocalDateTime sentAt) {
        this.id = id;
        this.chatRoom = chatRoom;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderRole = senderRole;
        this.type = type;
        this.content = content;
        this.translatedContent = translatedContent != null ? translatedContent : new HashMap<>();
        this.fileId = fileId;
        this.caption = caption;
        this.isSecure = isSecure;
        this.sentAt = sentAt != null ? sentAt : LocalDateTime.now();
    }
}
