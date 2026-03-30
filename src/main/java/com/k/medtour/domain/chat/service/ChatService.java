package com.k.medtour.domain.chat.service;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.admin.repository.MemberRepository;
import com.k.medtour.domain.chat.dto.ChatFileMessageResponse;
import com.k.medtour.domain.chat.dto.ChatMessageListResponse;
import com.k.medtour.domain.chat.dto.ChatMessageResponse;
import com.k.medtour.domain.chat.dto.ChatMessageSendRequest;
import com.k.medtour.domain.chat.dto.ChatRoomCreateRequest;
import com.k.medtour.domain.chat.dto.ChatRoomListResponse;
import com.k.medtour.domain.chat.dto.ChatRoomResponse;
import com.k.medtour.domain.chat.dto.MonitorResponse;
import com.k.medtour.domain.chat.dto.ReadRequest;
import com.k.medtour.domain.chat.dto.ReadResponse;
import com.k.medtour.domain.chat.dto.SosRequest;
import com.k.medtour.domain.chat.dto.SosResponse;
import com.k.medtour.domain.chat.entity.ChatMessage;
import com.k.medtour.domain.chat.entity.ChatRoom;
import com.k.medtour.domain.chat.entity.ChatRoomParticipant;
import com.k.medtour.domain.chat.enums.ChatMessageType;
import com.k.medtour.domain.chat.enums.ChatRoomType;
import com.k.medtour.domain.chat.repository.ChatMessageRepository;
import com.k.medtour.domain.chat.repository.ChatRoomParticipantRepository;
import com.k.medtour.domain.chat.repository.ChatRoomRepository;
import com.k.medtour.domain.file.dto.FileUploadResponse;
import com.k.medtour.domain.file.service.FileService;
import com.k.medtour.global.exception.BusinessException;
import com.k.medtour.global.exception.ErrorCode;
import com.k.medtour.infra.translation.TranslationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final MemberRepository memberRepository;
    private final FileService fileService;
    private final TranslationService translationService;

    /**
     * 채팅방 생성
     */
    public ChatRoomResponse createRoom(ChatRoomCreateRequest request) {
        List<Long> participantIds = request.participants().stream()
                .map(ChatRoomCreateRequest.ParticipantInfo::userId)
                .sorted()
                .toList();

        String roomId = generateRoomId(request.journeyId(), request.type(), participantIds);

        if (chatRoomRepository.existsByRoomId(roomId)) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_ALREADY_EXISTS);
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(roomId)
                .type(request.type())
                .journeyId(request.journeyId())
                .language(request.language())
                .build();

        chatRoomRepository.save(chatRoom);

        List<ChatRoomResponse.ParticipantResponse> participantResponses = new ArrayList<>();

        for (ChatRoomCreateRequest.ParticipantInfo info : request.participants()) {
            Member member = memberRepository.findById(info.userId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            ChatRoomParticipant participant = ChatRoomParticipant.builder()
                    .chatRoom(chatRoom)
                    .memberId(info.userId())
                    .build();
            participantRepository.save(participant);
            chatRoom.addParticipant(participant);

            participantResponses.add(new ChatRoomResponse.ParticipantResponse(
                    member.getId(), member.getName(), info.role()));
        }

        log.info("Chat room created: roomId={}, type={}, participants={}",
                roomId, request.type(), participantIds);

        return ChatRoomResponse.from(chatRoom, participantResponses);
    }

    /**
     * 내 채팅방 목록 조회
     */
    public List<ChatRoomListResponse> getMyRooms(Long memberId, ChatRoomType type,
                                                  int page, int size) {
        List<ChatRoom> rooms;

        if (type != null) {
            rooms = chatRoomRepository.findAllByParticipantMemberIdAndType(memberId, type, page, size);
        } else {
            rooms = chatRoomRepository.findAllByParticipantMemberId(memberId, page, size);
        }

        return rooms.stream()
                .map(room -> toChatRoomListResponse(room, memberId))
                .toList();
    }

    /**
     * 메시지 이력 조회 (커서 기반)
     */
    public ChatMessageListResponse getMessages(String roomId, String cursor, int size,
                                                Long memberId, String role) {
        validateParticipantOrAdmin(roomId, memberId, role);

        List<ChatMessage> messages;

        if (cursor != null && !cursor.isBlank()) {
            messages = messageRepository.findByRoomIdAndCursorOrderBySentAtDesc(roomId, cursor, size + 1);
        } else {
            messages = messageRepository.findByRoomIdOrderBySentAtDesc(roomId, size + 1);
        }

        boolean hasMore = messages.size() > size;
        if (hasMore) {
            messages = messages.subList(0, size);
        }

        List<Long> participantIds = participantRepository.findMemberIdsByRoomId(roomId);

        List<ChatMessageResponse> messageResponses = messages.stream()
                .map(msg -> ChatMessageResponse.from(msg, getReadByForMessage(roomId, msg.getId(), participantIds)))
                .toList();

        String nextCursor = messages.isEmpty() ? null : messages.get(messages.size() - 1).getId();

        return new ChatMessageListResponse(messageResponses, nextCursor, hasMore);
    }

    /**
     * 텍스트 메시지 전송
     */
    public ChatMessageResponse sendMessage(String roomId, ChatMessageSendRequest request,
                                           Long senderId, String senderRole) {
        validateParticipantOrAdmin(roomId, senderId, senderRole);

        ChatRoom chatRoom = findRoomOrThrow(roomId);
        Member sender = memberRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Map<String, String> translated = translationService.translate(
                request.content(),
                request.language() != null ? request.language() : "en",
                chatRoom.getLanguage() != null ? chatRoom.getLanguage() : "ko"
        );

        String messageId = UUID.randomUUID().toString();
        ChatMessage message = ChatMessage.builder()
                .id(messageId)
                .chatRoom(chatRoom)
                .senderId(senderId)
                .senderName(sender.getName())
                .senderRole(senderRole)
                .type(request.type())
                .content(request.content())
                .translatedContent(translated)
                .sentAt(LocalDateTime.now())
                .build();

        messageRepository.save(message);

        log.info("Message sent: roomId={}, messageId={}, senderId={}", roomId, messageId, senderId);

        List<Long> participantIds = participantRepository.findMemberIdsByRoomId(roomId);
        return ChatMessageResponse.from(message, getReadByForMessage(roomId, messageId, participantIds));
    }

    /**
     * 파일 메시지 전송
     */
    public ChatFileMessageResponse sendFileMessage(String roomId, InputStream fileInputStream,
                                                    String filename, String contentType, long fileSize,
                                                    String caption, Boolean isSecure,
                                                    Long senderId, String senderRole) {
        validateParticipantOrAdmin(roomId, senderId, senderRole);

        ChatRoom chatRoom = findRoomOrThrow(roomId);
        Member sender = memberRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        FileUploadResponse fileUpload;
        try {
            fileUpload = fileService.upload(fileInputStream, filename, contentType, fileSize,
                    "CHAT_FILE", senderId);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.CHAT_FILE_ERROR, e.getMessage());
        }

        String messageId = UUID.randomUUID().toString();
        ChatMessage message = ChatMessage.builder()
                .id(messageId)
                .chatRoom(chatRoom)
                .senderId(senderId)
                .senderName(sender.getName())
                .senderRole(senderRole)
                .type(ChatMessageType.FILE)
                .content(caption)
                .fileId(fileUpload.id())
                .caption(caption)
                .isSecure(isSecure != null ? isSecure : false)
                .sentAt(LocalDateTime.now())
                .build();

        messageRepository.save(message);

        log.info("File message sent: roomId={}, messageId={}, fileId={}", roomId, messageId, fileUpload.id());

        return new ChatFileMessageResponse(
                messageId,
                roomId,
                ChatMessageType.FILE,
                new ChatFileMessageResponse.FileInfo(
                        fileUpload.id(),
                        fileUpload.fileName(),
                        fileUpload.fileSize(),
                        fileUpload.mimeType(),
                        fileUpload.url(),
                        isSecure != null && isSecure,
                        LocalDateTime.now().plusDays(30)
                ),
                caption,
                message.getSentAt()
        );
    }

    /**
     * 읽음 처리
     */
    public ReadResponse markAsRead(String roomId, ReadRequest request,
                                    Long memberId, String role) {
        validateParticipantOrAdmin(roomId, memberId, role);

        ChatRoomParticipant participant = participantRepository
                .findByChatRoom_RoomIdAndMemberId(roomId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT));

        participant.updateLastReadMessageId(request.lastReadMessageId());

        long unreadCount = messageRepository.countUnreadMessages(roomId, request.lastReadMessageId());

        log.info("Read marked: roomId={}, memberId={}, lastReadMessageId={}",
                roomId, memberId, request.lastReadMessageId());

        return new ReadResponse(roomId, unreadCount);
    }

    /**
     * 관리자 멀티챗 관제 조회
     */
    public MonitorResponse monitorRooms(ChatRoomType type, int page, int size) {
        List<ChatRoom> rooms;
        long totalRooms;

        if (type != null) {
            rooms = chatRoomRepository.findAllByTypeOrderByUpdatedAtDesc(type, page, size);
            totalRooms = chatRoomRepository.countByType(type);
        } else {
            rooms = chatRoomRepository.findAllOrderByUpdatedAtDesc(page, size);
            totalRooms = chatRoomRepository.countAll();
        }

        long totalUnread = 0;
        List<MonitorResponse.MonitorRoomInfo> roomInfos = new ArrayList<>();

        for (ChatRoom room : rooms) {
            ChatMessage lastMsg = messageRepository
                    .findTopByChatRoom_RoomIdOrderBySentAtDesc(room.getRoomId())
                    .orElse(null);

            long unread = messageRepository.countByRoomId(room.getRoomId());

            MonitorResponse.MonitorLastMessage lastMessage = null;
            if (lastMsg != null) {
                String translatedFirst = lastMsg.getTranslatedContent() != null
                        && !lastMsg.getTranslatedContent().isEmpty()
                        ? lastMsg.getTranslatedContent().values().iterator().next()
                        : null;
                lastMessage = new MonitorResponse.MonitorLastMessage(
                        lastMsg.getContent(), translatedFirst, lastMsg.getSentAt());
            }

            boolean isUrgent = lastMsg != null && lastMsg.getType() == ChatMessageType.SOS;

            MonitorResponse.PatientInfo patient = findPatientInRoom(room);

            roomInfos.add(new MonitorResponse.MonitorRoomInfo(
                    room.getRoomId(), room.getType(), patient,
                    room.getJourneyId(), lastMessage, unread, isUrgent));

            totalUnread += unread;
        }

        int totalPages = (int) Math.ceil((double) totalRooms / size);

        return new MonitorResponse(
                totalRooms, rooms.size(), totalUnread,
                roomInfos, page, size, totalRooms, totalPages);
    }

    /**
     * 긴급 호출 (SOS)
     */
    public SosResponse sendSos(SosRequest request, Long senderId, String senderRole) {
        String roomId = request.roomId();
        ChatRoom chatRoom;

        if (roomId != null && !roomId.isBlank()) {
            chatRoom = findRoomOrThrow(roomId);
        } else {
            throw new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND, "SOS 전송 시 roomId가 필요합니다.");
        }

        Member sender = memberRepository.findById(senderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String messageId = UUID.randomUUID().toString();
        ChatMessage sosMessage = ChatMessage.builder()
                .id(messageId)
                .chatRoom(chatRoom)
                .senderId(senderId)
                .senderName(sender.getName())
                .senderRole(senderRole)
                .type(ChatMessageType.SOS)
                .content("[SOS] " + request.message())
                .sentAt(LocalDateTime.now())
                .build();

        messageRepository.save(sosMessage);

        log.warn("SOS sent: roomId={}, senderId={}, message={}", roomId, senderId, request.message());

        return new SosResponse(messageId, roomId, sosMessage.getContent(), sosMessage.getSentAt());
    }

    // ---- Private helpers ----

    private String generateRoomId(Long journeyId, ChatRoomType type, List<Long> participantIds) {
        String ids = participantIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("-"));
        String journeyPart = journeyId != null ? String.valueOf(journeyId) : "nj";
        return "room-" + journeyPart + "-" + type.name().toLowerCase() + "-" + ids;
    }

    private ChatRoom findRoomOrThrow(String roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private void validateParticipantOrAdmin(String roomId, Long memberId, String role) {
        if (isAdmin(role)) {
            return;
        }
        if (!participantRepository.existsByChatRoom_RoomIdAndMemberId(roomId, memberId)) {
            throw new BusinessException(ErrorCode.CHAT_NOT_PARTICIPANT);
        }
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role) || "MASTER".equalsIgnoreCase(role);
    }

    private ChatRoomListResponse toChatRoomListResponse(ChatRoom room, Long currentMemberId) {
        List<ChatRoomParticipant> participants = participantRepository
                .findAllByChatRoom_RoomId(room.getRoomId());

        ChatRoomParticipant otherParticipantEntity = participants.stream()
                .filter(p -> !p.getMemberId().equals(currentMemberId))
                .findFirst()
                .orElse(participants.isEmpty() ? null : participants.get(0));

        ChatRoomListResponse.OtherParticipant otherParticipant = null;
        if (otherParticipantEntity != null) {
            Member otherMember = memberRepository.findById(otherParticipantEntity.getMemberId())
                    .orElse(null);
            if (otherMember != null) {
                otherParticipant = new ChatRoomListResponse.OtherParticipant(
                        otherMember.getId(), otherMember.getName(), otherMember.getProfileImage());
            }
        }

        ChatMessage lastMsg = messageRepository
                .findTopByChatRoom_RoomIdOrderBySentAtDesc(room.getRoomId())
                .orElse(null);

        ChatRoomListResponse.LastMessageInfo lastMessageInfo = null;
        if (lastMsg != null) {
            boolean isTranslated = lastMsg.getTranslatedContent() != null
                    && !lastMsg.getTranslatedContent().isEmpty();
            lastMessageInfo = new ChatRoomListResponse.LastMessageInfo(
                    lastMsg.getContent(), lastMsg.getSentAt(), isTranslated);
        }

        ChatRoomParticipant myParticipant = participants.stream()
                .filter(p -> p.getMemberId().equals(currentMemberId))
                .findFirst()
                .orElse(null);

        long unreadCount = 0;
        if (myParticipant != null && myParticipant.getLastReadMessageId() != null) {
            unreadCount = messageRepository.countUnreadMessages(
                    room.getRoomId(), myParticipant.getLastReadMessageId());
        } else {
            unreadCount = messageRepository.countByRoomId(room.getRoomId());
        }

        return new ChatRoomListResponse(
                room.getRoomId(), room.getType(), otherParticipant,
                lastMessageInfo, unreadCount, room.getUpdatedAt());
    }

    private List<Long> getReadByForMessage(String roomId, String messageId,
                                            List<Long> participantIds) {
        List<ChatRoomParticipant> participants = participantRepository
                .findAllByChatRoom_RoomId(roomId);

        return participants.stream()
                .filter(p -> p.getLastReadMessageId() != null
                        && p.getLastReadMessageId().compareTo(messageId) >= 0)
                .map(ChatRoomParticipant::getMemberId)
                .toList();
    }

    private MonitorResponse.PatientInfo findPatientInRoom(ChatRoom room) {
        List<ChatRoomParticipant> participants = participantRepository
                .findAllByChatRoom_RoomId(room.getRoomId());

        for (ChatRoomParticipant p : participants) {
            Member member = memberRepository.findByIdWithRole(p.getMemberId()).orElse(null);
            if (member != null && "PATIENT".equalsIgnoreCase(member.getRole().getName())) {
                return new MonitorResponse.PatientInfo(member.getId(), member.getName());
            }
        }
        return null;
    }
}
