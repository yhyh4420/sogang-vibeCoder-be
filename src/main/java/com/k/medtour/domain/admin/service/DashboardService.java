package com.k.medtour.domain.admin.service;

import com.k.medtour.domain.admin.dto.DashboardOverviewResponse;
import com.k.medtour.domain.admin.dto.StaffStatusResponse;
import com.k.medtour.domain.admin.repository.MemberRepository;
import com.k.medtour.domain.chat.repository.ChatMessageRepository;
import com.k.medtour.domain.journey.entity.StaffAssignment;
import com.k.medtour.domain.journey.enums.JourneyStatus;
import com.k.medtour.domain.journey.enums.StaffAssignmentStatus;
import com.k.medtour.domain.journey.repository.JourneyRepository;
import com.k.medtour.domain.journey.repository.JourneyScheduleItemRepository;
import com.k.medtour.domain.journey.repository.StaffAssignmentRepository;
import com.k.medtour.domain.staff.entity.StaffProfile;
import com.k.medtour.domain.staff.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RequiredArgsConstructor
public class DashboardService {

    private final MemberRepository memberRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyScheduleItemRepository scheduleItemRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final ChatMessageRepository chatMessageRepository;

    public DashboardOverviewResponse getOverview() {
        long totalPatients = memberRepository.countByRoleName("PATIENT");

        long activeJourneys = journeyRepository.countByFilters(
                JourneyStatus.IN_PROGRESS, null, null, null);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        long todaySchedules = scheduleItemRepository.countByScheduledAtBetweenAndDeletedAtIsNull(
                startOfDay, endOfDay);

        long unreadChats = chatMessageRepository.countUnreadMessages();

        return new DashboardOverviewResponse(totalPatients, activeJourneys, todaySchedules, unreadChats);
    }

    public StaffStatusResponse getStaffStatus() {
        List<StaffProfile> allStaff = staffProfileRepository.findAll();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<StaffStatusResponse.StaffStatusItem> staffList = allStaff.stream()
                .map(sp -> {
                    List<StaffAssignment> todayAssignments = staffAssignmentRepository
                            .findByStaffIdAndDate(sp.getMember().getId(), startOfDay, endOfDay);

                    boolean hasOnDuty = todayAssignments.stream()
                            .anyMatch(a -> a.getStatus() == StaffAssignmentStatus.ON_DUTY);

                    String status;
                    if (!sp.getIsAvailable()) {
                        status = "OFFLINE";
                    } else if (hasOnDuty) {
                        status = "ON_DUTY";
                    } else {
                        status = "AVAILABLE";
                    }

                    return new StaffStatusResponse.StaffStatusItem(
                            sp.getMember().getId(),
                            sp.getMember().getName(),
                            sp.getStaffType().name(),
                            status
                    );
                })
                .toList();

        long available = staffList.stream().filter(s -> "AVAILABLE".equals(s.status())).count();
        long onDuty = staffList.stream().filter(s -> "ON_DUTY".equals(s.status())).count();
        long offline = staffList.stream().filter(s -> "OFFLINE".equals(s.status())).count();

        return new StaffStatusResponse(staffList.size(), available, onDuty, offline, staffList);
    }
}
