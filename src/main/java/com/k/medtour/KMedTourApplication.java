package com.k.medtour;

import com.k.medtour.global.auth.jwt.JwtTokenProvider;
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
        // Uncomment when DB is available:
        // var dataSource = DatabaseConfig.create(config);

        // 3. Initialize JWT provider
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
                config.jwtSecret(),
                config.accessTokenExpiration(),
                config.refreshTokenExpiration()
        );

        // 4. Create repositories (JDBC)
        // var memberRepo = new JdbcMemberRepository(dataSource);
        // ... other repositories

        // 5. Create services (manual DI)
        // var authService = new AuthService(memberRepo, jwtTokenProvider, ...);
        // ... other services

        // 6. Build router and register controllers
        Router router = new Router();
        router.addMiddleware(new JwtFilter(jwtTokenProvider));

        // Register controllers
        // new AuthController(authService).register(router);
        // ... other controllers

        // Health check endpoint
        router.get("/health", ctx -> "OK");

        // 7. Start server
        HttpServerApp server = new HttpServerApp(config.port(), router);
        server.start();
        log.info("K-MedTour server started on port {}", config.port());

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down server...");
            server.stop();
            // if (dataSource != null) dataSource.close();
        }));
    }
}
