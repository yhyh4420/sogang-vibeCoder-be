package com.k.medtour.domain.chat.dto;

import com.k.medtour.domain.chat.enums.ChatMessageType;

public record ChatMessageSendRequest(
        ChatMessageType type,

        String content,

        String language
) {
}
