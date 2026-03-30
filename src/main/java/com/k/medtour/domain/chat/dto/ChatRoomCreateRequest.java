package com.k.medtour.domain.chat.dto;

import com.k.medtour.domain.chat.enums.ChatRoomType;

import java.util.List;

public record ChatRoomCreateRequest(
        ChatRoomType type,

        List<ParticipantInfo> participants,

        Long journeyId,

        String language
) {
    public record ParticipantInfo(
            Long userId,
            String role
    ) {
    }
}
