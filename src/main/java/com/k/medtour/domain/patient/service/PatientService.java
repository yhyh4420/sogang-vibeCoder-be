package com.k.medtour.domain.patient.service;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.admin.repository.MemberRepository;
import com.k.medtour.domain.patient.dto.EmergencyContactRequest;
import com.k.medtour.domain.patient.dto.EmergencyContactResponse;
import com.k.medtour.domain.patient.dto.PassportRequest;
import com.k.medtour.domain.patient.dto.PassportResponse;
import com.k.medtour.domain.patient.dto.PatientListResponse;
import com.k.medtour.domain.patient.dto.QuestionnaireRequest;
import com.k.medtour.domain.patient.dto.QuestionnaireResponse;
import com.k.medtour.domain.patient.entity.EmergencyContact;
import com.k.medtour.domain.patient.entity.MedicalQuestionnaire;
import com.k.medtour.domain.patient.entity.PatientPassport;
import com.k.medtour.domain.patient.repository.EmergencyContactRepository;
import com.k.medtour.domain.patient.repository.MedicalQuestionnaireRepository;
import com.k.medtour.domain.patient.repository.PatientPassportRepository;
import com.k.medtour.global.auth.UserPrincipal;
import com.k.medtour.global.common.PageResponse;
import com.k.medtour.global.exception.BusinessException;
import com.k.medtour.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PatientService {

    private final PatientPassportRepository passportRepository;
    private final MedicalQuestionnaireRepository questionnaireRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final MemberRepository memberRepository;

    // ========== Passport ==========

    public PassportResponse createPassport(PassportRequest request, Long memberId) {
        if (passportRepository.existsByMemberIdAndDeletedAtIsNull(memberId)) {
            throw new BusinessException(ErrorCode.PASSPORT_ALREADY_EXISTS);
        }

        Member member = findMember(memberId);
        PatientPassport passport = PatientPassport.builder()
                .member(member)
                .passportNumber(request.passportNumber())
                .fullName(request.fullName())
                .nationality(request.nationality())
                .birthDate(request.birthDate())
                .expiryDate(request.expiryDate())
                .gender(request.gender())
                .inputType(request.inputType())
                .fileId(request.fileId())
                .ocrConfidence(request.ocrConfidence())
                .build();

        return PassportResponse.from(passportRepository.save(passport));
    }

    public PassportResponse getPassport(Long patientId) {
        PatientPassport passport = passportRepository.findByMemberIdAndDeletedAtIsNull(patientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_NOT_FOUND));
        return PassportResponse.from(passport);
    }

    public PassportResponse updatePassport(PassportRequest request, Long memberId) {
        PatientPassport passport = passportRepository.findByMemberIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSPORT_NOT_FOUND));

        passport.update(
                request.passportNumber(), request.fullName(), request.nationality(),
                request.birthDate(), request.expiryDate(), request.gender(),
                request.inputType(), request.fileId(), request.ocrConfidence()
        );

        return PassportResponse.from(passport);
    }

    // ========== Medical Questionnaire ==========

    public QuestionnaireResponse createQuestionnaire(QuestionnaireRequest request, Long memberId) {
        if (questionnaireRepository.existsByMemberIdAndDeletedAtIsNull(memberId)) {
            throw new BusinessException(ErrorCode.QUESTIONNAIRE_ALREADY_EXISTS);
        }

        Member member = findMember(memberId);
        MedicalQuestionnaire questionnaire = MedicalQuestionnaire.builder()
                .member(member)
                .bloodType(request.bloodType())
                .height(request.height())
                .weight(request.weight())
                .allergies(request.allergies())
                .currentMedications(request.currentMedications())
                .pastSurgeries(request.pastSurgeries())
                .chronicConditions(request.chronicConditions())
                .additionalNotes(request.additionalNotes())
                .build();

        return QuestionnaireResponse.from(questionnaireRepository.save(questionnaire));
    }

    public QuestionnaireResponse getQuestionnaire(Long patientId) {
        MedicalQuestionnaire questionnaire = questionnaireRepository
                .findByMemberIdAndDeletedAtIsNull(patientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTIONNAIRE_NOT_FOUND));
        return QuestionnaireResponse.from(questionnaire);
    }

    public QuestionnaireResponse updateQuestionnaire(QuestionnaireRequest request, Long memberId) {
        MedicalQuestionnaire questionnaire = questionnaireRepository
                .findByMemberIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTIONNAIRE_NOT_FOUND));

        questionnaire.update(
                request.bloodType(), request.height(), request.weight(),
                request.allergies(), request.currentMedications(),
                request.pastSurgeries(), request.chronicConditions(),
                request.additionalNotes()
        );

        return QuestionnaireResponse.from(questionnaire);
    }

    // ========== Emergency Contact ==========

    public EmergencyContactResponse createEmergencyContact(EmergencyContactRequest request, Long memberId) {
        Member member = findMember(memberId);

        EmergencyContact contact = EmergencyContact.builder()
                .member(member)
                .name(request.name())
                .relationship(request.relationship())
                .phone(request.phone())
                .email(request.email())
                .isPrimary(request.isPrimary())
                .build();

        return EmergencyContactResponse.from(emergencyContactRepository.save(contact));
    }

    public List<EmergencyContactResponse> getEmergencyContacts(Long patientId) {
        return emergencyContactRepository.findAllByMemberIdAndDeletedAtIsNull(patientId)
                .stream()
                .map(EmergencyContactResponse::from)
                .toList();
    }

    public EmergencyContactResponse updateEmergencyContact(Long contactId,
                                                            EmergencyContactRequest request,
                                                            Long memberId) {
        EmergencyContact contact = emergencyContactRepository
                .findByIdAndMemberIdAndDeletedAtIsNull(contactId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMERGENCY_CONTACT_NOT_FOUND));

        contact.update(request.name(), request.relationship(), request.phone(),
                request.email(), request.isPrimary());

        return EmergencyContactResponse.from(contact);
    }

    public void deleteEmergencyContact(Long contactId, Long memberId) {
        EmergencyContact contact = emergencyContactRepository
                .findByIdAndMemberIdAndDeletedAtIsNull(contactId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMERGENCY_CONTACT_NOT_FOUND));

        contact.softDelete();
    }

    // ========== Admin ==========

    public PageResponse<PatientListResponse> getPatientList(int page, int size) {
        List<Member> members = memberRepository.findAllByRoleName("PATIENT", page, size);
        long totalElements = memberRepository.countByRoleName("PATIENT");
        List<PatientListResponse> content = members.stream().map(member -> {
            boolean hasPassport = passportRepository.existsByMemberIdAndDeletedAtIsNull(member.getId());
            boolean hasQuestionnaire = questionnaireRepository.existsByMemberIdAndDeletedAtIsNull(member.getId());
            int contactCount = emergencyContactRepository
                    .findAllByMemberIdAndDeletedAtIsNull(member.getId()).size();
            return PatientListResponse.of(member, hasPassport, hasQuestionnaire, contactCount);
        }).toList();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    // ========== Helper ==========

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
