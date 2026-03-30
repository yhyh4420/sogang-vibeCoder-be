package com.k.medtour.domain.chat.controller;

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
import com.k.medtour.domain.chat.enums.ChatRoomType;
import com.k.medtour.domain.chat.service.ChatService;
import com.k.medtour.global.auth.UserPrincipal;
import com.k.medtour.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;

import java.io.InputStream;
import java.util.List;

@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 채팅방 생성
     */
    public ApiResponse<ChatRoomResponse> createRoom(ChatRoomCreateRequest request) {
        return ApiResponse.success("채팅방 생성 완료", chatService.createRoom(request));
    }

    /**
     * 내 채팅방 목록 조회
     */
    public ApiResponse<List<ChatRoomListResponse>> getMyRooms(
            UserPrincipal principal,
            ChatRoomType type,
            int page,
            int size) {
        return ApiResponse.success("조회 성공",
                chatService.getMyRooms(principal.memberId(), type, page, size));
    }

    /**
     * 메시지 이력 조회 (커서 기반)
     */
    public ApiResponse<ChatMessageListResponse> getMessages(
            String roomId,
            String cursor,
            int size,
            UserPrincipal principal) {
        return ApiResponse.success("조회 성공",
                chatService.getMessages(roomId, cursor, size, principal.memberId(), principal.role()));
    }

    /**
     * 텍스트 메시지 전송
     */
    public ApiResponse<ChatMessageResponse> sendMessage(
            String roomId,
            ChatMessageSendRequest request,
            UserPrincipal principal) {
        return ApiResponse.success("전송 완료",
                chatService.sendMessage(roomId, request, principal.memberId(), principal.role()));
    }

    /**
     * 파일 메시지 전송
     */
    public ApiResponse<ChatFileMessageResponse> sendFileMessage(
            String roomId,
            InputStream fileInputStream,
            String filename,
            String contentType,
            long fileSize,
            String caption,
            Boolean isSecure,
            UserPrincipal principal) {
        return ApiResponse.success("파일 전송 완료",
                chatService.sendFileMessage(roomId, fileInputStream, filename, contentType, fileSize,
                        caption, isSecure, principal.memberId(), principal.role()));
    }

    /**
     * 읽음 처리
     */
    public ApiResponse<ReadResponse> markAsRead(
            String roomId,
            ReadRequest request,
            UserPrincipal principal) {
        return ApiResponse.success("읽음 처리 완료",
                chatService.markAsRead(roomId, request, principal.memberId(), principal.role()));
    }

    /**
     * 관리자 멀티챗 관제 조회
     */
    public ApiResponse<MonitorResponse> monitorRooms(
            ChatRoomType type,
            int page,
            int size) {
        return ApiResponse.success("조회 성공", chatService.monitorRooms(type, page, size));
    }

    /**
     * 긴급 호출 (SOS)
     */
    public ApiResponse<SosResponse> sendSos(
            SosRequest request,
            UserPrincipal principal) {
        return ApiResponse.success("SOS 전송 완료",
                chatService.sendSos(request, principal.memberId(), principal.role()));
    }
}
