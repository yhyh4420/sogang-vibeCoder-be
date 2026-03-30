package com.k.medtour;

import com.k.medtour.domain.admin.controller.AuthController;
import com.k.medtour.domain.admin.controller.DashboardController;
import com.k.medtour.domain.admin.controller.MemberController;
import com.k.medtour.domain.admin.repository.*;
import com.k.medtour.domain.admin.service.AuthService;
import com.k.medtour.domain.admin.service.DashboardService;
import com.k.medtour.domain.admin.service.MemberService;
import com.k.medtour.domain.aftercare.controller.AftercareController;
import com.k.medtour.domain.aftercare.repository.*;
import com.k.medtour.domain.aftercare.service.AftercareService;
import com.k.medtour.domain.chat.controller.ChatController;
import com.k.medtour.domain.chat.repository.*;
import com.k.medtour.domain.chat.service.ChatService;
import com.k.medtour.domain.file.controller.FileController;
import com.k.medtour.domain.file.repository.JdbcFileRepository;
import com.k.medtour.domain.file.service.FileService;
import com.k.medtour.domain.journey.controller.JourneyController;
import com.k.medtour.domain.journey.controller.StaffJourneyController;
import com.k.medtour.domain.journey.repository.*;
import com.k.medtour.domain.journey.service.JourneyService;
import com.k.medtour.domain.journey.service.StaffAssignmentService;
import com.k.medtour.domain.notification.controller.NotificationController;
import com.k.medtour.domain.notification.repository.JdbcNotificationRepository;
import com.k.medtour.domain.notification.service.NotificationService;
import com.k.medtour.domain.patient.controller.PatientController;
import com.k.medtour.domain.patient.repository.*;
import com.k.medtour.domain.patient.service.PatientService;
import com.k.medtour.domain.profile.controller.ProfileController;
import com.k.medtour.domain.profile.service.ProfileService;
import com.k.medtour.domain.proposal.controller.ProposalController;
import com.k.medtour.domain.proposal.repository.*;
import com.k.medtour.domain.proposal.service.ProposalService;
import com.k.medtour.domain.staff.repository.JdbcStaffProfileRepository;
import com.k.medtour.global.auth.jwt.JwtProperties;
import com.k.medtour.global.auth.jwt.JwtTokenProvider;
import com.k.medtour.infra.s3.LocalStorageService;
import com.k.medtour.infra.translation.NoOpTranslationService;
import com.k.medtour.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KMedTourApplication {

    private static final Logger log = LoggerFactory.getLogger(KMedTourApplication.class);

    public static void main(String[] args) throws Exception {
        // 1. Load configuration
        AppConfig config = AppConfig.load();
        log.info("Configuration loaded: port={}", config.port());

        // 2. Initialize database (HikariCP + Flyway)
        var dataSource = DatabaseConfig.create(config);

        // 3. Initialize JWT provider
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
                config.jwtSecret(),
                config.accessTokenExpiration(),
                config.refreshTokenExpiration()
        );
        JwtProperties jwtProperties = new JwtProperties(
                config.jwtSecret(),
                config.accessTokenExpiration(),
                config.refreshTokenExpiration()
        );

        // 4. Create repositories (JDBC)
        var memberRepo = new JdbcMemberRepository(dataSource);
        var roleRepo = new JdbcRoleRepository(dataSource);
        var magicLinkRepo = new JdbcMagicLinkRepository(dataSource);
        var refreshTokenRepo = new JdbcRefreshTokenRepository(dataSource);
        var memberConsentRepo = new JdbcMemberConsentRepository(dataSource);
        var agencyProfileRepo = new JdbcAgencyProfileRepository(dataSource);
        var staffProfileRepo = new JdbcStaffProfileRepository(dataSource);
        var passportRepo = new JdbcPatientPassportRepository(dataSource);
        var questionnaireRepo = new JdbcMedicalQuestionnaireRepository(dataSource);
        var emergencyContactRepo = new JdbcEmergencyContactRepository(dataSource);
        var fileRepo = new JdbcFileRepository(dataSource);
        var proposalRepo = new JdbcProposalRepository(dataSource);
        var proposalItemRepo = new JdbcProposalItemRepository(dataSource);
        var proposalRequestRepo = new JdbcProposalRequestRepository(dataSource);
        var journeyRepo = new JdbcJourneyRepository(dataSource);
        var scheduleItemRepo = new JdbcJourneyScheduleItemRepository(dataSource);
        var templateRepo = new JdbcJourneyTemplateRepository(dataSource);
        var templateItemRepo = new JdbcJourneyTemplateItemRepository(dataSource);
        var staffAssignmentRepo = new JdbcStaffAssignmentRepository(dataSource);
        var chatRoomRepo = new JdbcChatRoomRepository(dataSource);
        var chatMessageRepo = new JdbcChatMessageRepository(dataSource);
        var chatParticipantRepo = new JdbcChatRoomParticipantRepository(dataSource);
        var notificationRepo = new JdbcNotificationRepository(dataSource);
        var guideRepo = new JdbcAftercareGuideRepository(dataSource);
        var invoiceRepo = new JdbcInvoiceRepository(dataSource);
        var staffReportRepo = new JdbcStaffReportRepository(dataSource);

        // 5. Infra services
        var storageService = new LocalStorageService("uploads", "http://localhost:" + config.port() + "/files");
        var translationService = new NoOpTranslationService();

        // 6. Create services (manual DI)
        var authService = new AuthService(
                memberRepo, roleRepo, magicLinkRepo, refreshTokenRepo,
                memberConsentRepo, jwtTokenProvider, jwtProperties
        );
        var memberService = new MemberService(memberRepo, staffProfileRepo, agencyProfileRepo);
        var dashboardService = new DashboardService(
                memberRepo, journeyRepo, scheduleItemRepo,
                staffAssignmentRepo, staffProfileRepo, chatMessageRepo
        );
        var patientService = new PatientService(passportRepo, questionnaireRepo, emergencyContactRepo, memberRepo);
        var fileService = new FileService(fileRepo, storageService);
        var proposalService = new ProposalService(proposalRepo, proposalRequestRepo, memberRepo);
        var journeyService = new JourneyService(
                templateRepo, journeyRepo, scheduleItemRepo,
                staffAssignmentRepo, memberRepo, staffProfileRepo
        );
        var staffAssignmentService = new StaffAssignmentService(
                staffAssignmentRepo, scheduleItemRepo, journeyRepo,
                memberRepo, staffProfileRepo, questionnaireRepo, emergencyContactRepo
        );
        var chatService = new ChatService(
                chatRoomRepo, chatParticipantRepo, chatMessageRepo,
                memberRepo, fileService, translationService
        );
        var notificationService = new NotificationService(notificationRepo, memberRepo);
        var aftercareService = new AftercareService(guideRepo, invoiceRepo, staffReportRepo, journeyRepo);
        var profileService = new ProfileService(agencyProfileRepo, fileRepo);

        // 7. Build router and register controllers
        Router router = new Router();
        router.addMiddleware(new JwtFilter(jwtTokenProvider));

        new AuthController(authService).register(router);
        new MemberController(memberService).register(router);
        new DashboardController(dashboardService).register(router);
        new PatientController(patientService).register(router);
        new FileController(fileService).register(router);
        new ProposalController(proposalService).register(router);
        new JourneyController(journeyService).register(router);
        new StaffJourneyController(staffAssignmentService).register(router);
        new ChatController(chatService).register(router);
        new NotificationController(notificationService).register(router);
        new AftercareController(aftercareService).register(router);
        new ProfileController(profileService).register(router);

        // Health check endpoint
        router.get("/health", ctx -> "OK");

        // 8. Start server
        HttpServerApp server = new HttpServerApp(config.port(), router);
        server.start();
        log.info("K-MedTour server started on port {}", config.port());

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down server...");
            server.stop();
            dataSource.close();
        }));
    }
}
