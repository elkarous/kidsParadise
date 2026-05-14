package com.kids.reports;

import com.kids.services.FinancialReportingService.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.*;

/**
 * Export Engine — Blueprint for report export.
 *
 * Strategy pattern: the same report DTO can be exported to
 *   • CSV  (fast, zero dependencies, useful for Excel)
 *   • PDF  (via JasperReports — replace template path with your .jrxml)
 *   • TXT  (simple formatted table, good for WhatsApp/print)
 *
 * All text output is UTF-8 to correctly render Arabic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportExportService {

    // ═══════════════════════════════════════════════════════════════════════
    //  CSV Export
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Export the teacher payroll report to a UTF-8 CSV file.
     *
     * The BOM (0xEF 0xBB 0xBF) prefix ensures Excel opens Arabic text correctly
     * on Windows without manual encoding selection.
     */
    public Path exportTeacherPayrollCsv(TeacherPayrollReport report, Path outputDir) throws IOException {

        Path file = outputDir.resolve("teacher-payroll-%s.csv".formatted(report.month()));

        try (var writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file.toFile()), StandardCharsets.UTF_8))) {

            // Write UTF-8 BOM for Excel compatibility
            writer.write('\uFEFF');

            // Header row (bilingual)
            writer.write("الاسم / Name,نوع الراتب / Salary Type," +
                         "الراتب الإجمالي / Gross,المدفوع / Paid," +
                         "المتبقي / Pending,الحالة / Status\n");

            for (TeacherSalaryLine line : report.lines()) {
                writer.write(String.join(",",
                    escapeCsv(line.teacherName()),
                    line.salaryType().name(),
                    line.grossSalary().toPlainString(),
                    line.amountPaid().toPlainString(),
                    line.pendingAmount().toPlainString(),
                    line.fullyPaid() ? "مدفوع / PAID" : "متبقي / PENDING"
                ));
                writer.newLine();
            }

            // Summary footer
            writer.write("\n");
            writer.write("الإجمالي / TOTAL,,");
            writer.write(report.totalGross().toPlainString() + ",");
            writer.write(report.totalPaid().toPlainString() + ",");
            writer.write(report.totalPending().toPlainString() + ",\n");
        }

        log.info("Teacher payroll CSV exported to {}", file);
        return file;
    }

    /**
     * Export the student revenue report to CSV.
     */
    public Path exportStudentRevenueCsv(StudentRevenueReport report, Path outputDir) throws IOException {

        Path file = outputDir.resolve("student-revenue-%s.csv".formatted(report.month()));

        try (var writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file.toFile()), StandardCharsets.UTF_8))) {

            writer.write('\uFEFF'); // BOM

            writer.write("""
                الشهر / Month,%s
                إجمالي الإيرادات / Total Revenue,%s
                المتأخرات / Outstanding,%s
                الإجمالي المتوقع / Total Expected,%s
                نسبة التحصيل / Collection Rate,%.1f%%
                عدد الطلاب الذين دفعوا / Students Paid,%d
                عدد الطلاب بمتأخرات / Students With Balance,%d
                """.formatted(
                    report.month(),
                    report.totalRevenue().toPlainString(),
                    report.outstandingBalance().toPlainString(),
                    report.totalExpected().toPlainString(),
                    report.collectionRate(),
                    report.studentsPaid(),
                    report.studentsWithBalance()
            ));
        }

        log.info("Student revenue CSV exported to {}", file);
        return file;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Plain Text Export (for printing / WhatsApp sharing)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Generate a formatted plain-text summary suitable for printing or sharing.
     * Uses Unicode box-drawing characters for a clean table look.
     */
    public String generateTextSummary(DashboardSnapshot snapshot) {
        return """
            ┌─────────────────────────────────────────┐
            │     Garden School — Financial Summary    │
            │         ملخص مالي — روضة الأمل          │
            ├─────────────────────────────────────────┤
            │  Month / الشهر   : %-22s│
            ├─────────────────────────────────────────┤
            │  Income / المدخول : %-22s│
            │  Expenses/ المصروف: %-22s│
            │  Net / الصافي    : %-22s│
            └─────────────────────────────────────────┘
            """.formatted(
                snapshot.month(),
                snapshot.income().toPlainString() + " DT",
                snapshot.expenses().toPlainString() + " DT",
                snapshot.netProfit().toPlainString() + " DT"
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PDF Export via JasperReports (Blueprint)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Export the teacher payroll report to PDF using a JasperReports template.
     *
     * SETUP STEPS:
     * 1. Add to pom.xml:
     *    <dependency>
     *      <groupId>net.sf.jasperreports</groupId>
     *      <artifactId>jasperreports</artifactId>
     *      <version>6.21.0</version>
     *    </dependency>
     *
     * 2. Place your .jrxml template in src/main/resources/reports/
     *    The template references these field names (matching TeacherSalaryLine):
     *      $F{teacherName}, $F{salaryType}, $F{grossSalary}, $F{amountPaid},
     *      $F{pendingAmount}, $F{fullyPaid}
     *
     * 3. Set the font to a Unicode-capable font (e.g. "Arial Unicode MS" or
     *    embed a custom font extension) to render Arabic correctly in PDF.
     *
     * @param report     the payroll report to render
     * @param outputDir  directory where the PDF will be saved
     * @return           path to the generated PDF file
     */
    public Path exportTeacherPayrollPdf(TeacherPayrollReport report, Path outputDir)
            throws JRException, IOException {

        // ── 1. Compile the template (cache in production) ─────────────────
        InputStream templateStream = getClass()
            .getResourceAsStream("/reports/teacher-payroll.jrxml");

        if (templateStream == null) {
            throw new FileNotFoundException(
                "JasperReports template not found: /reports/teacher-payroll.jrxml"
            );
        }

        JasperReport compiledReport = JasperCompileManager.compileReport(templateStream);

        // ── 2. Build the data source from our DTOs ────────────────────────
        JRBeanCollectionDataSource dataSource =
            new JRBeanCollectionDataSource(report.lines());

        // ── 3. Set report parameters ──────────────────────────────────────
        Map<String, Object> params = new HashMap<>();
        params.put("REPORT_MONTH",    report.month().toString());
        params.put("TOTAL_GROSS",     report.totalGross().toPlainString() + " DT");
        params.put("TOTAL_PAID",      report.totalPaid().toPlainString()  + " DT");
        params.put("TOTAL_PENDING",   report.totalPending().toPlainString() + " DT");
        params.put("SCHOOL_NAME",     "روضة الأمل / Garden School");
        params.put("REPORT_LOCALE",   new Locale("ar", "TN"));

        // ── 4. Fill and export ────────────────────────────────────────────
        JasperPrint print = JasperFillManager.fillReport(compiledReport, params, dataSource);

        Path file = outputDir.resolve("teacher-payroll-%s.pdf".formatted(report.month()));
        JasperExportManager.exportReportToPdfFile(print, file.toString());

        log.info("Teacher payroll PDF exported to {}", file);
        return file;
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    private String escapeCsv(String value) {
        if (value == null) return "";
        // Wrap in quotes if it contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
