package com.k.medtour.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application configuration loaded from app.properties and environment variables.
 * Environment variables take precedence over properties file values.
 */
public record AppConfig(
        int port,
        String dbUrl,
        String dbUsername,
        String dbPassword,
        String jwtSecret,
        long accessTokenExpiration,
        long refreshTokenExpiration
) {

    public static AppConfig load() {
        Properties props = new Properties();

        // Load from classpath
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            // Properties file is optional; environment variables can provide everything
        }

        int port = intVal(props, "server.port", "SERVER_PORT", 8080);
        String dbUrl = stringVal(props, "db.url", "DB_URL", "jdbc:postgresql://localhost:5432/kmedtour");
        String dbUsername = stringVal(props, "db.username", "DB_USERNAME", "postgres");
        String dbPassword = stringVal(props, "db.password", "DB_PASSWORD", "postgres");
        String jwtSecret = stringVal(props, "jwt.secret", "JWT_SECRET",
                "default-secret-key-for-development-only-change-in-production-32bytes");
        long accessExp = longVal(props, "jwt.access-token-expiration", "JWT_ACCESS_TOKEN_EXPIRATION", 1800000L);
        long refreshExp = longVal(props, "jwt.refresh-token-expiration", "JWT_REFRESH_TOKEN_EXPIRATION", 604800000L);

        return new AppConfig(port, dbUrl, dbUsername, dbPassword, jwtSecret, accessExp, refreshExp);
    }

    private static String stringVal(Properties props, String propKey, String envKey, String defaultValue) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) return env;
        return props.getProperty(propKey, defaultValue);
    }

    private static int intVal(Properties props, String propKey, String envKey, int defaultValue) {
        String val = stringVal(props, propKey, envKey, null);
        if (val == null) return defaultValue;
        return Integer.parseInt(val.trim());
    }

    private static long longVal(Properties props, String propKey, String envKey, long defaultValue) {
        String val = stringVal(props, propKey, envKey, null);
        if (val == null) return defaultValue;
        return Long.parseLong(val.trim());
    }
}
