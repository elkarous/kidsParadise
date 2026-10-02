package com.kindererp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Exports all of the school's data to one Excel workbook (one sheet per table), so a school can always
 * take its data elsewhere. Money columns are converted back to normal amounts. Password hashes and the
 * logo image are never exported.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    /** Table name and the translation key of its sheet title, in workbook order. */
    public static final List<String[]> TABLES = List.of(
            new String[]{"students", "export.sheet.students"},
            new String[]{"parents", "export.sheet.parents"},
            new String[]{"levels", "export.sheet.levels"},
            new String[]{"school_classes", "export.sheet.classes"},
            new String[]{"school_years", "export.sheet.schoolYears"},
            new String[]{"working_months", "export.sheet.months"},
            new String[]{"tuition_payments", "export.sheet.tuitionPayments"},
            new String[]{"student_attendance", "export.sheet.studentAttendance"},
            new String[]{"teachers", "export.sheet.teachers"},
            new String[]{"employees", "export.sheet.employees"},
            new String[]{"teacher_attendance", "export.sheet.teacherAttendance"},
            new String[]{"employee_attendance", "export.sheet.employeeAttendance"},
            new String[]{"teacher_payments", "export.sheet.teacherPayments"},
            new String[]{"employee_payments", "export.sheet.employeePayments"},
            new String[]{"school_settings", "export.sheet.settings"},
            new String[]{"users", "export.sheet.users"});

    private static final Set<String> MONEY_COLUMNS = Set.of("monthly_fee", "discount_two_children",
            "discount_three_plus_children", "base_salary", "absence_penalty", "amount", "discount", "gross_amount");
    private static final Set<String> NEVER_EXPORTED = Set.of("password_hash", "logo", "license_key");

    private final JdbcTemplate jdbc;

    public static String suggestedFileName() {
        return "kindererp-export-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")) + ".xlsx";
    }

    /** @param sheetTitle translates a sheet key (the UI passes I18n::get) */
    @Transactional(readOnly = true)
    public Path exportAll(Path target, Function<String, String> sheetTitle) {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle header = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            header.setFont(bold);
            CellStyle money = workbook.createCellStyle();
            money.setDataFormat(workbook.createDataFormat().getFormat("#,##0.000"));

            for (String[] table : TABLES) {
                writeSheet(workbook, table[0], sheetTitle.apply(table[1]), header, money);
            }
            Files.createDirectories(target.toAbsolutePath().getParent());
            try (OutputStream out = Files.newOutputStream(target)) {
                workbook.write(out);
            }
        } catch (IOException e) {
            log.error("Excel export failed", e);
            throw new BusinessException("export.error.write", target);
        }
        log.info("Data exported to {}", target);
        return target;
    }

    private void writeSheet(Workbook workbook, String table, String title, CellStyle header, CellStyle money) {
        List<Map<String, Object>> rows = jdbc.queryForList("select * from " + table + " order by 1");
        List<String> columns = jdbc.query("select name from pragma_table_info(?) order by cid",
                (rs, i) -> rs.getString(1), table).stream().filter(c -> !NEVER_EXPORTED.contains(c)).toList();

        Sheet sheet = workbook.createSheet(safeSheetName(title));
        Row headerRow = sheet.createRow(0);
        for (int c = 0; c < columns.size(); c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(columns.get(c));
            cell.setCellStyle(header);
        }
        int r = 1;
        for (Map<String, Object> values : rows) {
            Row row = sheet.createRow(r++);
            for (int c = 0; c < columns.size(); c++) {
                String column = columns.get(c);
                Object value = values.get(column);
                Cell cell = row.createCell(c);
                if (value == null) {
                    continue;
                }
                if (MONEY_COLUMNS.contains(column) && value instanceof Number number) {
                    cell.setCellValue(BigDecimal.valueOf(number.longValue(), 3).doubleValue());
                    cell.setCellStyle(money);
                } else if (value instanceof Number number) {
                    cell.setCellValue(number.doubleValue());
                } else {
                    cell.setCellValue(value.toString());
                }
            }
        }
        for (int c = 0; c < columns.size(); c++) {
            sheet.autoSizeColumn(c);
        }
        sheet.createFreezePane(0, 1);
    }

    /** Excel sheet names: max 31 characters, no []:*?/\ characters. */
    private static String safeSheetName(String title) {
        String cleaned = title.replaceAll("[\\[\\]:*?/\\\\]", " ").trim();
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }
}
