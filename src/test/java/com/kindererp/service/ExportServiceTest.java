package com.kindererp.service;

import com.kindererp.IntegrationTest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExportServiceTest extends IntegrationTest {

    @Autowired
    private ExportService exportService;
    @Autowired
    private UserService userService;
    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    void exportsEveryTableWithReadableAmountsAndWithoutPasswords(@TempDir Path dir) throws Exception {
        settings();
        student("Ali", parent("Famille"), schoolClass("A"));
        userService.createUser("admin", null, "secret123");
        entityManager.flush();

        Path file = exportService.exportAll(dir.resolve("export.xlsx"), key -> key.substring(key.lastIndexOf('.') + 1));

        try (InputStream in = Files.newInputStream(file); XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(ExportService.TABLES.size());
            assertThat(workbook.getSheet("students").getRow(1).getCell(1).getStringCellValue()).isEqualTo("Ali");

            Sheet settings = workbook.getSheet("settings");
            int feeColumn = headers(settings).indexOf("monthly_fee");
            assertThat(settings.getRow(1).getCell(feeColumn).getNumericCellValue()).isEqualTo(100.0);

            assertThat(headers(workbook.getSheet("users"))).doesNotContain("password_hash");
            assertThat(headers(settings)).doesNotContain("logo", "license_key");
        }
    }

    private static List<String> headers(Sheet sheet) {
        List<String> names = new ArrayList<>();
        Row header = sheet.getRow(0);
        header.forEach(cell -> names.add(cell.getStringCellValue()));
        return names;
    }
}
