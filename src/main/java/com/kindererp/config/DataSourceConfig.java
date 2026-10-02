package com.kindererp.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SQLite connection pool. Every connection gets foreign keys enforced (off by default in SQLite),
 * WAL journaling and a busy timeout so concurrent readers/writers wait instead of failing.
 * The schema is created and upgraded by Flyway (db/migration) on startup.
 */
@Configuration
public class DataSourceConfig {

    @Bean(destroyMethod = "close")
    public DataSource dataSource(@Value("${kindererp.db:}") String configuredPath) throws IOException {
        Path dbFile = configuredPath.isBlank() ? AppPaths.dbFile() : Path.of(configuredPath).toAbsolutePath();
        Files.createDirectories(dbFile.getParent());

        HikariConfig config = new HikariConfig();
        config.setPoolName("kindererp-sqlite");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile);
        config.setMaximumPoolSize(4);
        config.addDataSourceProperty("foreign_keys", "true");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("busy_timeout", "10000");
        return new HikariDataSource(config);
    }
}
