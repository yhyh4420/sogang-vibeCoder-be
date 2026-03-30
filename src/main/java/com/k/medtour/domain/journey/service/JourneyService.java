package com.k.medtour.domain.journey.service;

import com.k.medtour.domain.admin.entity.Member;
import com.k.medtour.domain.admin.repository.MemberRepository;
import com.k.medtour.domain.journey.dto.JourneyCreateRequest;
import com.k.medtour.domain.journey.dto.JourneyDetailResponse;
import com.k.medtour.domain.journey.dto.JourneyListResponse;
import com.k.medtour.domain.journey.dto.JourneyResponse;
import com.k.medtour.domain.journey.dto.LocationDto;
import com.k.medtour.domain.journey.dto.ScheduleItemCreateRequest;
import com.k.medtour.domain.journey.dto.ScheduleItemResponse;
import com.k.medtour.domain.journey.dto.ScheduleItemUpdateRequest;
import com.k.medtour.domain.journey.dto.StaffAssignRequest;
import com.k.medtour.domain.journey.dto.TemplateCreateRequest;
import com.k.medtour.domain.journey.dto.TemplateItemDto;
import com.k.medtour.domain.journey.dto.TemplateListResponse;
import com.k.medtour.domain.journey.dto.TemplateResponse;
import com.k.medtour.domain.journey.dto.TimelineResponse;
import com.k.medtour.domain.journey.entity.Journey;
import com.k.medtour.domain.journey.entity.JourneyScheduleItem;
import com.k.medtour.domain.journey.entity.JourneyTemplate;
import com.k.medtour.domain.journey.entity.JourneyTemplateItem;
import com.k.medtour.domain.journey.entity.StaffAssignment;
import com.k.medtour.domain.journey.enums.JourneyStatus;
import com.k.medtour.domain.journey.enums.ScheduleItemType;
import com.k.medtour.domain.journey.enums.TemplateCategory;
import com.k.medtour.domain.journey.repository.JourneyRepository;
import com.k.medtour.domain.journey.repository.JourneyScheduleItemRepository;
import com.k.medtour.domain.journey.repository.JourneyTemplateRepository;
import com.k.medtour.domain.journey.repository.StaffAssignmentRepository;
import com.k.medtour.domain.staff.entity.StaffProfile;
import com.k.medtour.domain.staff.repository.StaffProfileRepository;
import com.k.medtour.global.auth.UserPrincipal;
import com.k.medtour.global.common.PageResponse;
import com.k.medtour.global.exception.BusinessException;
import com.k.medtour.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class JourneyService {

    private final JourneyTemplateRepository templateRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyScheduleItemRepository scheduleItemRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final MemberRepository memberRepository;
    private final StaffProfileRepository staffProfileRepository;

    // ======================== Template CRUD ========================

    public PageResponse<TemplateListResponse> getTemplates(String keyword, TemplateCategory category, int page, int size) {
        List<JourneyTemplate> templates = templateRepository.findAllByFilters(keyword, category, page, size);
        long totalElements = templateRepository.countByFilters(keyword, category);
        List<TemplateListResponse> content = templates.stream().map(TemplateListResponse::from).toList();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    public TemplateResponse getTemplate(Long templateId) {
        JourneyTemplate template = findTemplateOrThrow(templateId);
        return TemplateResponse.from(template);
    }

    public TemplateResponse createTemplate(TemplateCreateRequest request) {
        if (templateRepository.existsByName(request.name())) {
            throw new BusinessException(ErrorCode.TEMPLATE_DUPLICATE_NAME);
        }

        JourneyTemplate template = JourneyTemplate.builder()
                .name(request.name())
                .category(request.category())
                .durationDays(request.durationDays())
                .build();

        if (request.items() != null && !request.items().isEmpty()) {
            AtomicInteger sortOrder = new AtomicInteger(1);
            List<JourneyTemplateItem> items = request.items().stream()
                    .map(dto -> JourneyTemplateItem.builder()
                            .dayOffset(dto.dayOffset())
                            .timeOffset(dto.timeOffset())
                            .title(dto.title())
                            .type(dto.type())
                            .description(dto.description())
                            .durationMinutes(dto.durationMinutes())
                            .location(dto.location() != null ? dto.location().toMap() : null)
                            .requiredStaff(dto.requiredStaff())
                            .sortOrder(sortOrder.getAndIncrement())
                            .build())
                    .toList();
            template.replaceItems(items);
        }

        templateRepository.save(template);
        return TemplateResponse.from(template);
    }

    public TemplateResponse updateTemplate(Long templateId, TemplateCreateRequest request) {
        JourneyTemplate template = findTemplateOrThrow(templateId);

        if (templateRepository.existsByNameAndIdNot(request.name(), templateId)) {
            throw new BusinessException(ErrorCode.TEMPLATE_DUPLICATE_NAME);
        }

        template.update(request.name(), request.category(), request.durationDays());

        if (request.items() != null) {
            AtomicInteger sortOrder = new AtomicInteger(1);
            List<JourneyTemplateItem> items = request.items().stream()
                    .map(dto -> JourneyTemplateItem.builder()
                            .dayOffset(dto.dayOffset())
                            .timeOffset(dto.timeOffset())
                            .title(dto.title())
                            .type(dto.type())
                            .description(dto.description())
                            .durationMinutes(dto.durationMinutes())
                            .location(dto.location() != null ? dto.location().toMap() : null)
                            .requiredStaff(dto.requiredStaff())
                            .sortOrder(sortOrder.getAndIncrement())
                            .build())
                    .toList();
            template.replaceItems(items);
        }

        return TemplateResponse.from(template);
    }

    public void deleteTemplate(Long templateId) {
        JourneyTemplate template = findTemplateOrThrow(templateId);
        template.softDelete();
    }

    // ======================== Journey CRUD ========================

    public JourneyResponse createJourney(JourneyCreateRequest request) {
        Member patient = memberRepository.findById(request.patientId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PATIENT_NOT_FOUND));

        JourneyTemplate template = findTemplateOrThrow(request.templateId());

        boolean hasActive = journeyRepository.existsByPatientIdAndStatusIn(
                request.patientId(),
                List.of(JourneyStatus.PLANNED, JourneyStatus.IN_PROGRESS)
        );
        if (hasActive) {
            throw new BusinessException(ErrorCode.ACTIVE_JOURNEY_EXISTS);
        }

        LocalDate endDate = request.startDate().plusDays(template.getDurationDays() - 1);

        Journey journey = Journey.builder()
                .patient(patient)
                .title(request.title())
                .startDate(request.startDate())
                .endDate(endDate)
                .notes(request.notes())
                .build();

        for (JourneyTemplateItem templateItem : template.getItems()) {
            LocalDate itemDate = request.startDate().plusDays(templateItem.getDayOffset());
            String[] timeParts = templateItem.getTimeOffset().split(":");
            LocalTime time = LocalTime.of(Integer.parseInt(timeParts[0]), Integer.parseInt(timeParts[1]));
            LocalDateTime scheduledAt = LocalDateTime.of(itemDate, time);
            int dayNumber = templateItem.getDayOffset() + 1;

            JourneyScheduleItem scheduleItem = JourneyScheduleItem.builder()
                    .journey(journey)
                    .dayNumber(dayNumber)
                    .scheduledAt(scheduledAt)
                    .title(templateItem.getTitle())
                    .type(templateItem.getType())
                    .description(templateItem.getDescription())
                    .durationMinutes(templateItem.getDurationMinutes())
                    .location(templateItem.getLocation())
                    .build();

            journey.addScheduleItem(scheduleItem);
        }

        template.incrementUsageCount();
        journeyRepository.save(journey);

        return JourneyResponse.from(journey);
    }

    public PageResponse<JourneyListResponse> getJourneys(JourneyStatus status, Long patientId,
                                                          LocalDate startDateFrom, LocalDate startDateTo,
                                                          int page, int size) {
        List<Journey> journeys = journeyRepository.findAllByFilters(status, patientId, startDateFrom, startDateTo, page, size);
        long totalElements = journeyRepository.countByFilters(status, patientId, startDateFrom, startDateTo);
        List<JourneyListResponse> content = journeys.stream().map(JourneyListResponse::from).toList();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    public JourneyDetailResponse getJourneyDetail(Long journeyId, UserPrincipal principal) {
        Journey journey = findJourneyOrThrow(journeyId);
        validateJourneyAccess(journey, principal);
        return JourneyDetailResponse.from(journey);
    }

    // ======================== Schedule Item CRUD ========================

    public ScheduleItemResponse addScheduleItem(Long journeyId, ScheduleItemCreateRequest request) {
        Journey journey = findJourneyOrThrow(journeyId);

        int dayNumber = (int) ChronoUnit.DAYS.between(journey.getStartDate(),
                request.scheduledAt().toLocalDate()) + 1;

        JourneyScheduleItem item = JourneyScheduleItem.builder()
                .journey(journey)
                .dayNumber(dayNumber)
                .scheduledAt(request.scheduledAt())
                .title(request.title())
                .type(request.type())
                .description(request.description())
                .durationMinutes(request.durationMinutes())
                .location(request.location() != null ? request.location().toMap() : null)
                .build();

        journey.addScheduleItem(item);
        scheduleItemRepository.save(item);

        return ScheduleItemResponse.from(item);
    }

    public ScheduleItemResponse updateScheduleItem(Long journeyId, Long itemId, ScheduleItemUpdateRequest request) {
        JourneyScheduleItem item = findScheduleItemOrThrow(journeyId, itemId);

        item.update(
                request.scheduledAt(),
                request.title(),
                request.description(),
                request.durationMinutes(),
                request.location() != null ? request.location().toMap() : null
        );

        return ScheduleItemResponse.from(item);
    }

    public void deleteScheduleItem(Long journeyId, Long itemId) {
        JourneyScheduleItem item = findScheduleItemOrThrow(journeyId, itemId);
        if (!item.isModifiable()) {
            throw new BusinessException(ErrorCode.SCHEDULE_ITEM_COMPLETED,
                    "이미 진행/완료된 일정은 삭제할 수 없습니다.");
        }
        scheduleItemRepository.deleteById(item.getId());
    }

    // ======================== Staff Assignment ========================

    public void assignStaff(Long journeyId, StaffAssignRequest request) {
        Journey journey = findJourneyOrThrow(journeyId);
        Member staff = memberRepository.findById(request.staffId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STAFF_NOT_FOUND));

        for (Long scheduleItemId : request.scheduleItemIds()) {
            JourneyScheduleItem item = scheduleItemRepository.findByIdAndJourneyId(scheduleItemId, journeyId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_ITEM_NOT_FOUND));

            if (!staffAssignmentRepository.existsByScheduleItemIdAndStaffId(scheduleItemId, request.staffId())) {
                StaffAssignment assignment = StaffAssignment.builder()
                        .scheduleItem(item)
                        .staff(staff)
                        .build();
                staffAssignmentRepository.save(assignment);
            }
        }
    }

    // ======================== Patient Timeline ========================

    public TimelineResponse getTimeline(Long patientId, LocalDate date) {
        List<Journey> journeys = journeyRepository.findByPatientIdAndStatusIn(
                patientId, List.of(JourneyStatus.PLANNED, JourneyStatus.IN_PROGRESS));

        if (journeys.isEmpty()) {
            throw new BusinessException(ErrorCode.JOURNEY_NOT_FOUND, "활성 여정이 없습니다.");
        }

        Journey journey = journeys.getFirst();

        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();

        List<JourneyScheduleItem> items = scheduleItemRepository
                .findByJourneyIdAndDate(journey.getId(), startOfDay, endOfDay);

        int dayNumber = (int) ChronoUnit.DAYS.between(journey.getStartDate(), targetDate) + 1;
        int totalDays = (int) ChronoUnit.DAYS.between(journey.getStartDate(), journey.getEndDate()) + 1;

        List<TimelineResponse.TimelineItemDto> timelineItems = items.stream()
                .map(item -> {
                    LocationDto loc = LocationDto.from(item.getLocation());
                    TimelineResponse.TimelineLocationDto locationDto = loc != null
                            ? new TimelineResponse.TimelineLocationDto(loc.name(), loc.googleMapsUrl())
                            : null;

                    Map<String, TimelineResponse.TimelineStaffDto> staffMap = new LinkedHashMap<>();
                    for (StaffAssignment sa : item.getStaffAssignments()) {
                        StaffProfile profile = staffProfileRepository.findByMemberId(sa.getStaff().getId())
                                .orElse(null);
                        String key = profile != null ? profile.getStaffType().name().toLowerCase() : "staff";
                        staffMap.put(key, new TimelineResponse.TimelineStaffDto(
                                sa.getStaff().getName(),
                                sa.getStaff().getPhone()
                        ));
                    }

                    return new TimelineResponse.TimelineItemDto(
                            item.getId(),
                            item.getScheduledAt(),
                            item.getTitle(),
                            item.getType().name(),
                            item.getStatus().name(),
                            item.getDescription(),
                            locationDto,
                            staffMap
                    );
                })
                .toList();

        return new TimelineResponse(
                journey.getId(),
                journey.getTitle(),
                targetDate,
                dayNumber,
                totalDays,
                timelineItems
        );
    }

    // ======================== Private Helpers ========================

    private JourneyTemplate findTemplateOrThrow(Long templateId) {
        return templateRepository.findByIdAndDeletedAtIsNull(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));
    }

    private Journey findJourneyOrThrow(Long journeyId) {
        return journeyRepository.findByIdAndDeletedAtIsNull(journeyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOURNEY_NOT_FOUND));
    }

    private JourneyScheduleItem findScheduleItemOrThrow(Long journeyId, Long itemId) {
        return scheduleItemRepository.findByIdAndJourneyId(itemId, journeyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_ITEM_NOT_FOUND));
    }

    private void validateJourneyAccess(Journey journey, UserPrincipal principal) {
        String role = principal.role();
        if ("ADMIN".equals(role) || "MASTER".equals(role)) {
            return;
        }
        if ("PATIENT".equals(role)) {
            if (!journey.getPatient().getId().equals(principal.memberId())) {
                throw new BusinessException(ErrorCode.JOURNEY_ACCESS_DENIED);
            }
            return;
        }
        if ("STAFF".equals(role)) {
            boolean assigned = staffAssignmentRepository.existsByJourneyIdAndStaffId(
                    journey.getId(), principal.memberId());
            if (!assigned) {
                throw new BusinessException(ErrorCode.JOURNEY_ACCESS_DENIED);
            }
            return;
        }
        throw new BusinessException(ErrorCode.JOURNEY_ACCESS_DENIED);
    }
}
