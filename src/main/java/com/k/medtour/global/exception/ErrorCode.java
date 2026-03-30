package com.k.medtour.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(400, "COMMON_001", "잘못된 입력입니다."),
    RESOURCE_NOT_FOUND(404, "COMMON_002", "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "COMMON_003", "서버 내부 오류가 발생했습니다."),
    DUPLICATE_RESOURCE(409, "COMMON_004", "이미 존재하는 리소스입니다."),

    // Auth
    UNAUTHORIZED(401, "AUTH_001", "인증이 필요합니다."),
    INVALID_OAUTH_TOKEN(400, "AUTH_001", "유효하지 않은 OAuth 토큰입니다."),
    UNSUPPORTED_OAUTH_PROVIDER(400, "AUTH_002", "지원하지 않는 OAuth 제공자입니다."),
    OAUTH_EMAIL_CONFLICT(409, "AUTH_003", "이미 다른 제공자로 가입된 이메일입니다."),
    TOKEN_EXPIRED(401, "AUTH_002", "토큰이 만료되었습니다."),
    INVALID_TOKEN(401, "AUTH_003", "유효하지 않은 토큰입니다."),
    ACCESS_DENIED(403, "AUTH_004", "접근 권한이 없습니다."),
    INVALID_MAGIC_LINK_TARGET(400, "AUTH_010", "유효하지 않은 이메일/전화번호 형식입니다."),
    MAGIC_LINK_RATE_LIMIT(429, "AUTH_011", "매직 링크 발급 횟수를 초과했습니다. (분당 3회)"),
    MAGIC_LINK_EXPIRED(400, "AUTH_020", "만료된 매직 링크입니다."),
    MAGIC_LINK_BIRTH_DATE_MISMATCH(401, "AUTH_021", "2차 인증(생년월일)이 일치하지 않습니다."),
    MAGIC_LINK_NOT_FOUND(404, "AUTH_022", "존재하지 않는 매직 링크 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(401, "AUTH_030", "만료된 Refresh Token입니다."),
    INVALID_REFRESH_TOKEN(401, "AUTH_031", "유효하지 않은 Refresh Token입니다."),
    CONSENT_REQUIRED_FIELDS(400, "AUTH_040", "필수 동의 항목이 체크되지 않았습니다."),
    ROLE_NOT_FOUND(400, "AUTH_051", "존재하지 않는 역할 ID입니다."),
    CANNOT_CHANGE_OWN_ROLE(403, "AUTH_052", "자기 자신의 역할은 변경할 수 없습니다."),

    // Member
    MEMBER_NOT_FOUND(404, "MEMBER_001", "회원을 찾을 수 없습니다."),

    // Patient
    PATIENT_NOT_FOUND(404, "PATIENT_001", "환자를 찾을 수 없습니다."),
    PASSPORT_NOT_FOUND(404, "PATIENT_002", "여권 정보를 찾을 수 없습니다."),
    PASSPORT_ALREADY_EXISTS(409, "PATIENT_003", "여권 정보가 이미 존재합니다."),
    QUESTIONNAIRE_NOT_FOUND(404, "PATIENT_004", "문진표를 찾을 수 없습니다."),
    QUESTIONNAIRE_ALREADY_EXISTS(409, "PATIENT_005", "문진표가 이미 존재합니다."),
    EMERGENCY_CONTACT_NOT_FOUND(404, "PATIENT_006", "긴급 연락처를 찾을 수 없습니다."),
    PATIENT_ACCESS_DENIED(403, "PATIENT_007", "본인의 데이터만 접근할 수 있습니다."),

    // Staff
    STAFF_NOT_FOUND(404, "STAFF_001", "실무자를 찾을 수 없습니다."),
    STAFF_PROFILE_NOT_FOUND(404, "STAFF_002", "실무자 프로필을 찾을 수 없습니다."),

    // Agency
    AGENCY_PROFILE_NOT_FOUND(404, "AGENCY_001", "에이전시 프로필을 찾을 수 없습니다."),

    // Journey
    JOURNEY_NOT_FOUND(404, "JOURNEY_001", "여정을 찾을 수 없습니다."),
    JOURNEY_ACCESS_DENIED(403, "JOURNEY_002", "해당 여정에 대한 접근 권한이 없습니다."),
    TEMPLATE_NOT_FOUND(404, "JOURNEY_003", "여정 템플릿을 찾을 수 없습니다."),
    TEMPLATE_DUPLICATE_NAME(409, "JOURNEY_004", "동일한 이름의 템플릿이 이미 존재합니다."),
    SCHEDULE_ITEM_NOT_FOUND(404, "JOURNEY_005", "일정 항목을 찾을 수 없습니다."),
    SCHEDULE_ITEM_COMPLETED(400, "JOURNEY_006", "이미 완료된 일정은 수정할 수 없습니다."),
    INVALID_STATUS_TRANSITION(400, "JOURNEY_007", "유효하지 않은 상태 전이입니다."),
    STAFF_NOT_ASSIGNED(403, "JOURNEY_008", "해당 일정에 배정되지 않은 실무자입니다."),
    ACTIVE_JOURNEY_EXISTS(409, "JOURNEY_009", "해당 환자에게 이미 진행 중인 여정이 존재합니다."),
    TEMPLATE_IN_USE(409, "JOURNEY_010", "활성 여정에서 사용 중인 템플릿은 삭제할 수 없습니다."),

    // Proposal
    PROPOSAL_NOT_FOUND(404, "PROPOSAL_001", "견적서를 찾을 수 없습니다."),
    PROPOSAL_ACCESS_DENIED(403, "PROPOSAL_002", "본인의 견적서만 접근할 수 있습니다."),
    PROPOSAL_INVALID_STATUS(400, "PROPOSAL_003", "유효하지 않은 견적서 상태 전이입니다."),
    PROPOSAL_ALREADY_RESPONDED(400, "PROPOSAL_004", "이미 응답한 견적서입니다."),

    // Chat
    CHAT_ROOM_NOT_FOUND(404, "CHAT_001", "채팅방을 찾을 수 없습니다."),
    CHAT_ROOM_ALREADY_EXISTS(409, "CHAT_002", "동일 참여자 간 이미 채팅방이 존재합니다."),
    CHAT_NOT_PARTICIPANT(403, "CHAT_003", "해당 채팅방 참여자가 아닙니다."),
    CHAT_FILE_ERROR(400, "CHAT_004", "채팅 파일 처리 중 오류가 발생했습니다."),

    // File
    FILE_UPLOAD_FAILED(500, "FIL_000", "파일 업로드에 실패했습니다."),
    FILE_SIZE_EXCEEDED(400, "FIL_001", "파일 크기가 20MB를 초과했습니다."),
    INVALID_FILE_TYPE(400, "FIL_002", "지원하지 않는 파일 형식입니다."),
    INVALID_FILE_CATEGORY(400, "FIL_003", "유효하지 않은 파일 카테고리입니다."),
    FILE_ACCESS_DENIED(403, "FIL_010", "파일 접근 권한이 없습니다."),
    FILE_NOT_FOUND(404, "FIL_011", "존재하지 않는 파일입니다."),

    // Notification
    NOTIFICATION_NOT_FOUND(404, "NOTI_001", "알림을 찾을 수 없습니다."),

    // Aftercare
    GUIDE_NOT_FOUND(404, "AFTER_001", "사후 관리 가이드를 찾을 수 없습니다."),
    GUIDE_ALREADY_EXISTS(409, "AFTER_002", "해당 여정의 사후 관리 가이드가 이미 존재합니다."),
    INVOICE_NOT_FOUND(404, "AFTER_003", "인보이스를 찾을 수 없습니다."),
    INVOICE_ALREADY_EXISTS(409, "AFTER_004", "해당 여정의 인보이스가 이미 존재합니다."),
    REPORT_ALREADY_EXISTS(409, "AFTER_005", "해당 여정의 업무 종료 리포트가 이미 존재합니다."),

    // Profile
    ORGANIZATION_NOT_FOUND(404, "PROFILE_001", "조직 프로필을 찾을 수 없습니다.");

    private final int httpStatusCode;
    private final String code;
    private final String message;
}
