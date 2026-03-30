package com.k.medtour.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public final class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    private DatabaseConfig() {
    }

    /**
     * Creates a HikariCP DataSource and runs Flyway migrations.
     */
    public static HikariDataSource create(AppConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.dbUrl());
        hikariConfig.setUsername(config.dbUsername());
        hikariConfig.setPassword(config.dbPassword());
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30_000);
        hikariConfig.setIdleTimeout(600_000);
        hikariConfig.setMaxLifetime(1_800_000);
        hikariConfig.setPoolName("kmedtour-pool");

        HikariDataSource dataSource = new HikariDataSource(hikariConfig);
        log.info("HikariCP connection pool created: {}", config.dbUrl());

        runMigrations(dataSource);
        return dataSource;
    }

    private static void runMigrations(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        var result = flyway.migrate();
        log.info("Flyway migration complete: {} migrations applied", result.migrationsExecuted);
    }
}
