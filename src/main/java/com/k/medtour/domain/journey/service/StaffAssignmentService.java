package com.k.medtour.domain.journey.service;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.admin.repository.MemberRepository;
import com.k.medtour.domain.journey.dto.LocationDto;
import com.k.medtour.domain.journey.dto.NavigationResponse;
import com.k.medtour.domain.journey.dto.PatientNoticeResponse;
import com.k.medtour.domain.journey.dto.StaffTodayResponse;
import com.k.medtour.domain.journey.dto.StatusUpdateRequest;
import com.k.medtour.domain.journey.dto.StatusUpdateResponse;
import com.k.medtour.domain.journey.entity.Journey;
import com.k.medtour.domain.journey.entity.JourneyScheduleItem;
import com.k.medtour.domain.journey.entity.StaffAssignment;
import com.k.medtour.domain.journey.enums.ScheduleItemStatus;
import com.k.medtour.domain.journey.enums.StaffAssignmentStatus;
import com.k.medtour.domain.journey.repository.JourneyRepository;
import com.k.medtour.domain.journey.repository.JourneyScheduleItemRepository;
import com.k.medtour.domain.journey.repository.StaffAssignmentRepository;
import com.k.medtour.domain.patient.entity.EmergencyContact;
import com.k.medtour.domain.patient.entity.MedicalQuestionnaire;
import com.k.medtour.domain.patient.repository.EmergencyContactRepository;
import com.k.medtour.domain.patient.repository.MedicalQuestionnaireRepository;
import com.k.medtour.domain.staff.entity.StaffProfile;
import com.k.medtour.domain.staff.repository.StaffProfileRepository;
import com.k.medtour.global.exception.BusinessException;
import com.k.medtour.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class StaffAssignmentService {

    private final StaffAssignmentRepository staffAssignmentRepository;
    private final JourneyScheduleItemRepository scheduleItemRepository;
    private final JourneyRepository journeyRepository;
    private final MemberRepository memberRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final MedicalQuestionnaireRepository questionnaireRepository;
    private final EmergencyContactRepository emergencyContactRepository;

    // ======================== Staff Today Tasks ========================

    public StaffTodayResponse getTodayTasks(Long staffId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

        List<StaffAssignment> assignments = staffAssignmentRepository
                .findByStaffIdAndDate(staffId, startOfDay, endOfDay);

        int completedCount = (int) assignments.stream()
                .filter(a -> a.getStatus() == StaffAssignmentStatus.COMPLETED)
                .count();

        List<StaffTodayResponse.TaskDto> tasks = assignments.stream()
                .map(sa -> {
                    JourneyScheduleItem item = sa.getScheduleItem();
                    Journey journey = item.getJourney();
                    Member patient = journey.getPatient();

                    return new StaffTodayResponse.TaskDto(
                            sa.getId(),
                            item.getId(),
                            journey.getId(),
                            item.getScheduledAt(),
                            item.getTitle(),
                            item.getType().name(),
                            sa.getStatus().name(),
                            new StaffTodayResponse.PatientDto(
                                    patient.getId(),
                                    patient.getName(),
                                    patient.getLanguage()
                            ),
                            LocationDto.from(item.getLocation()),
                            item.getDurationMinutes(),
                            sa.getCompletedAt()
                    );
                })
                .toList();

        return new StaffTodayResponse(targetDate, tasks.size(), completedCount, tasks);
    }

    // ======================== Patient Notice ========================

    public PatientNoticeResponse getPatientNotice(Long journeyId, Long staffId) {
        // Verify staff is assigned to this journey
        boolean assigned = staffAssignmentRepository.existsByJourneyIdAndStaffId(journeyId, staffId);
        if (!assigned) {
            throw new BusinessException(ErrorCode.STAFF_NOT_ASSIGNED);
        }

        Journey journey = journeyRepository.findByIdAndDeletedAtIsNull(journeyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOURNEY_NOT_FOUND));

        Member patient = journey.getPatient();

        // Get medical questionnaire for allergies
        MedicalQuestionnaire questionnaire = questionnaireRepository
                .findByMemberIdAndDeletedAtIsNull(patient.getId())
                .orElse(null);

        List<String> allergies = questionnaire != null ? questionnaire.getAllergies() : List.of();

        // Get emergency contact
        List<EmergencyContact> contacts = emergencyContactRepository
                .findAllByMemberIdAndDeletedAtIsNull(patient.getId());

        PatientNoticeResponse.EmergencyContactDto emergencyContactDto = null;
        if (!contacts.isEmpty()) {
            EmergencyContact primary = contacts.stream()
                    .filter(EmergencyContact::getIsPrimary)
                    .findFirst()
                    .orElse(contacts.getFirst());

            emergencyContactDto = new PatientNoticeResponse.EmergencyContactDto(
                    primary.getName(),
                    primary.getRelationship(),
                    primary.getPhone()
            );
        }

        return new PatientNoticeResponse(
                patient.getId(),
                patient.getName(),
                patient.getLanguage(),
                patient.getLanguage(),
                allergies,
                journey.getNotes(),
                emergencyContactDto
        );
    }

    // ======================== Status Update ========================

    public StatusUpdateResponse updateStatus(Long journeyId, Long itemId, Long staffId,
                                              StatusUpdateRequest request) {
        // Verify staff assignment
        JourneyScheduleItem item = scheduleItemRepository.findByIdAndJourneyId(itemId, journeyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_ITEM_NOT_FOUND));

        StaffAssignment assignment = staffAssignmentRepository
                .findByScheduleItemIdAndStaffId(itemId, staffId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STAFF_NOT_ASSIGNED));

        ScheduleItemStatus previousStatus = item.getStatus();

        // Update schedule item status (validates transition)
        item.updateStatus(request.status());

        // Update staff assignment status accordingly
        switch (request.status()) {
            case EN_ROUTE, ARRIVED, IN_PROGRESS -> assignment.startDuty();
            case COMPLETED -> assignment.complete();
            default -> { /* no change for SCHEDULED or CANCELLED */ }
        }

        Member staff = memberRepository.findById(staffId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STAFF_NOT_FOUND));

        return new StatusUpdateResponse(
                item.getId(),
                previousStatus,
                request.status(),
                LocalDateTime.now(),
                new StatusUpdateResponse.UpdatedByDto(staff.getId(), staff.getName())
        );
    }

    // ======================== Navigation Deep Links ========================

    public NavigationResponse getNavigation(Long journeyId, Long itemId, String platform) {
        JourneyScheduleItem item = scheduleItemRepository.findByIdAndJourneyId(itemId, journeyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_ITEM_NOT_FOUND));

        LocationDto location = LocationDto.from(item.getLocation());
        if (location == null || location.latitude() == null || location.longitude() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "위치 정보가 없는 일정입니다.");
        }

        double lat = location.latitude();
        double lng = location.longitude();
        String name = location.name() != null ? location.name() : "";

        Map<String, String> deepLinks = new LinkedHashMap<>();
        deepLinks.put("google", "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng);
        deepLinks.put("kakao", "kakaomap://route?ep=" + lat + "," + lng);
        deepLinks.put("naver", "nmap://route/car?dlat=" + lat + "&dlng=" + lng + "&dname=" + name);
        deepLinks.put("apple", "maps://?daddr=" + lat + "," + lng);

        return new NavigationResponse(
                new NavigationResponse.DestinationDto(name, lat, lng),
                deepLinks
        );
    }
}
