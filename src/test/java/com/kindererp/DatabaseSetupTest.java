package com.kindererp;

import com.kindererp.service.BackupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseSetupTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private BackupService backupService;
    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    void everyConnectionEnforcesForeignKeysAndUsesWal() {
        assertThat(jdbc.queryForObject("pragma foreign_keys", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("pragma journal_mode", String.class)).isEqualToIgnoringCase("wal");
    }

    @Test
    void foreignKeysAreActuallyChecked() {
        assertThatThrownBy(() -> jdbc.update(
                "insert into students (first_name, last_name, birth_date, enrollment_date, class_id) values ('a', 'b', '2020-01-01', '2025-09-01', 999)"))
                .hasMessageContaining("FOREIGN KEY");
    }

    @Test
    void moneyIsStoredAsIntegerThousandths() {
        settings();
        entityManager.flush();
        assertThat(jdbc.queryForObject("select typeof(monthly_fee) || ':' || monthly_fee from school_settings", String.class))
                .isEqualTo("integer:100000");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void backupProducesARestorableKinderErpDatabase(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        Path backup = backupService.backupTo(dir.resolve("copy.db"));

        assertThat(Files.size(backup)).isPositive();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + backup)) {
            assertThat(connection.createStatement()
                    .executeQuery("select count(*) from flyway_schema_history where success = 1").getInt(1)).isPositive();
        }
    }

    @Test
    void restoreRejectsFilesThatAreNotBackups(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        Path notADatabase = Files.writeString(dir.resolve("notes.db"), "hello");
        assertThatThrownBy(() -> backupService.scheduleRestore(notADatabase)).hasMessage("backup.error.invalidFile");
    }
}
