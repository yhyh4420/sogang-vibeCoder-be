package com.k.medtour.domain.patient.controller;

import com.k.medtour.domain.patient.dto.EmergencyContactRequest;
import com.k.medtour.domain.patient.dto.EmergencyContactResponse;
import com.k.medtour.domain.patient.dto.PassportRequest;
import com.k.medtour.domain.patient.dto.PassportResponse;
import com.k.medtour.domain.patient.dto.PatientListResponse;
import com.k.medtour.domain.patient.dto.QuestionnaireRequest;
import com.k.medtour.domain.patient.dto.QuestionnaireResponse;
import com.k.medtour.domain.patient.service.PatientService;
import com.k.medtour.global.auth.UserPrincipal;
import com.k.medtour.global.common.ApiResponse;
import com.k.medtour.global.common.PageResponse;
import com.k.medtour.server.Router;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    public void register(Router router) {
        router.post("/api/v1/patients/passport", ctx -> createPassport(ctx.body(PassportRequest.class), ctx.userPrincipal()));
        router.get("/api/v1/patients/{patientId}/passport", ctx -> getPassport(ctx.pathParamAsLong("patientId")));
        router.put("/api/v1/patients/passport", ctx -> updatePassport(ctx.body(PassportRequest.class), ctx.userPrincipal()));
        router.post("/api/v1/patients/questionnaire", ctx -> createQuestionnaire(ctx.body(QuestionnaireRequest.class), ctx.userPrincipal()));
        router.get("/api/v1/patients/{patientId}/questionnaire", ctx -> getQuestionnaire(ctx.pathParamAsLong("patientId")));
        router.put("/api/v1/patients/questionnaire", ctx -> updateQuestionnaire(ctx.body(QuestionnaireRequest.class), ctx.userPrincipal()));
        router.post("/api/v1/patients/emergency-contacts", ctx -> createEmergencyContact(ctx.body(EmergencyContactRequest.class), ctx.userPrincipal()));
        router.get("/api/v1/patients/{patientId}/emergency-contacts", ctx -> getEmergencyContacts(ctx.pathParamAsLong("patientId")));
        router.put("/api/v1/patients/emergency-contacts/{contactId}", ctx -> updateEmergencyContact(
                ctx.pathParamAsLong("contactId"), ctx.body(EmergencyContactRequest.class), ctx.userPrincipal()));
        router.delete("/api/v1/patients/emergency-contacts/{contactId}", ctx -> deleteEmergencyContact(
                ctx.pathParamAsLong("contactId"), ctx.userPrincipal()));
        router.get("/api/v1/admin/patients", ctx -> getPatientList(
                ctx.queryParamAsInt("page", 0), ctx.queryParamAsInt("size", 20)));
    }

    // ========== Passport ==========

    public ApiResponse<PassportResponse> createPassport(
            PassportRequest request, UserPrincipal principal) {
        PassportResponse response = patientService.createPassport(request, principal.memberId());
        return ApiResponse.success("여권 정보 등록 완료", response);
    }

    public ApiResponse<PassportResponse> getPassport(Long patientId) {
        return ApiResponse.success("조회 성공", patientService.getPassport(patientId));
    }

    public ApiResponse<PassportResponse> updatePassport(
            PassportRequest request, UserPrincipal principal) {
        return ApiResponse.success("수정 완료", patientService.updatePassport(request, principal.memberId()));
    }

    // ========== Medical Questionnaire ==========

    public ApiResponse<QuestionnaireResponse> createQuestionnaire(
            QuestionnaireRequest request, UserPrincipal principal) {
        QuestionnaireResponse response = patientService.createQuestionnaire(request, principal.memberId());
        return ApiResponse.success("문진표 등록 완료", response);
    }

    public ApiResponse<QuestionnaireResponse> getQuestionnaire(Long patientId) {
        return ApiResponse.success("조회 성공", patientService.getQuestionnaire(patientId));
    }

    public ApiResponse<QuestionnaireResponse> updateQuestionnaire(
            QuestionnaireRequest request, UserPrincipal principal) {
        return ApiResponse.success("수정 완료", patientService.updateQuestionnaire(request, principal.memberId()));
    }

    // ========== Emergency Contact ==========

    public ApiResponse<EmergencyContactResponse> createEmergencyContact(
            EmergencyContactRequest request, UserPrincipal principal) {
        EmergencyContactResponse response = patientService.createEmergencyContact(request, principal.memberId());
        return ApiResponse.success("긴급 연락처 등록 완료", response);
    }

    public ApiResponse<List<EmergencyContactResponse>> getEmergencyContacts(Long patientId) {
        return ApiResponse.success("조회 성공", patientService.getEmergencyContacts(patientId));
    }

    public ApiResponse<EmergencyContactResponse> updateEmergencyContact(
            Long contactId, EmergencyContactRequest request, UserPrincipal principal) {
        return ApiResponse.success("수정 완료",
                patientService.updateEmergencyContact(contactId, request, principal.memberId()));
    }

    public ApiResponse<Void> deleteEmergencyContact(Long contactId, UserPrincipal principal) {
        patientService.deleteEmergencyContact(contactId, principal.memberId());
        return ApiResponse.success("삭제 완료", null);
    }

    // ========== Admin ==========

    public ApiResponse<PageResponse<PatientListResponse>> getPatientList(int page, int size) {
        return ApiResponse.success("조회 성공", patientService.getPatientList(page, size));
    }
}
