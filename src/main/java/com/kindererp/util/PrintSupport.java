package com.kindererp.util;

import com.kindererp.model.SchoolSettings;
import com.kindererp.model.TuitionPayment;
import com.kindererp.service.dto.PayrollRow;
import com.kindererp.model.WorkingMonth;
import javafx.geometry.Insets;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Printed documents (receipts, pay slips) with the school's logo, name and contact details on top. */
public final class PrintSupport {

    private PrintSupport() {
    }

    public static void printTuitionReceipt(Window owner, TuitionPayment payment) {
        GridPane body = grid();
        row(body, "print.receipt.number", String.format("%06d", payment.getId()));
        row(body, "print.date", Formats.date(payment.getPaymentDate()));
        row(body, "parent.fatherName", payment.getParent().getFatherName());
        row(body, "payment.month", Formats.month(payment.getWorkingMonth()));
        row(body, "parent.childCount", String.valueOf(payment.getChildrenCount()));
        row(body, "payment.baseAmount", Formats.money(payment.getAmount().add(payment.getDiscount())));
        row(body, "payment.discount", Formats.money(payment.getDiscount()));
        row(body, "payment.total", Formats.money(payment.getAmount()));
        row(body, "payment.method", I18n.get("paymentMethod." + payment.getMethod().name()));
        if (payment.getNotes() != null) {
            row(body, "payment.notes", payment.getNotes());
        }
        print(owner, I18n.get("print.receipt.title"), body);
    }

    public static void printPaySlip(Window owner, PayrollRow row, WorkingMonth month, BigDecimal amountPaidNow) {
        GridPane body = grid();
        row(body, "print.date", Formats.date(LocalDate.now()));
        row(body, "staff.name", row.name());
        if (row.position() != null) {
            row(body, "staff.position", row.position());
        }
        row(body, "payment.month", Formats.month(month));
        row(body, "payroll.gross", Formats.money(row.gross()));
        row(body, "payroll.paidNow", Formats.money(amountPaidNow));
        row(body, "payroll.paidTotal", Formats.money(row.paid()));
        row(body, "payroll.pending", Formats.money(row.pending()));
        print(owner, I18n.get("print.payslip.title"), body);
    }

    /** Wraps {@code body} with the school header and a signature footer, then shows the print dialog. */
    public static void print(Window owner, String title, Node body) {
        VBox document = new VBox(16, header(), new Separator(), titleLabel(title), body, footer());
        document.getStyleClass().add("print-document");
        document.setPadding(new Insets(24));
        document.setPrefWidth(520);

        Scene scene = new Scene(document);
        scene.getStylesheets().add(Objects.requireNonNull(PrintSupport.class.getResource(ViewLoader.STYLESHEET)).toExternalForm());
        scene.setNodeOrientation(I18n.orientation());
        document.applyCss();
        document.layout();

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            Dialogs.warn(owner, I18n.get("print.error.noPrinter"));
            return;
        }
        if (!job.showPrintDialog(owner)) {
            job.cancelJob();
            return;
        }
        PageLayout layout = job.getJobSettings().getPageLayout();
        double scale = Math.min(1.0, layout.getPrintableWidth() / document.getBoundsInParent().getWidth());
        document.getTransforms().add(new Scale(scale, scale));
        if (job.printPage(document)) {
            job.endJob();
        } else {
            Dialogs.error(owner, I18n.get("print.error.failed"));
        }
    }

    private static Node header() {
        SchoolSettings settings = AppState.settings();
        ImageView logo = new ImageView(AppState.logo());
        logo.setFitHeight(64);
        logo.setFitWidth(64);
        logo.setPreserveRatio(true);

        Label name = new Label(AppState.schoolName());
        name.getStyleClass().add("print-school-name");
        VBox details = new VBox(2, name);
        if (settings.getAddress() != null) {
            details.getChildren().add(new Label(settings.getAddress()));
        }
        String contact = String.join("   ", nonNull(settings.getPhone()), nonNull(settings.getEmail())).trim();
        if (!contact.isEmpty()) {
            details.getChildren().add(new Label(contact));
        }
        return new HBox(16, logo, details);
    }

    private static Label titleLabel(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("print-title");
        return label;
    }

    private static Node footer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label signature = new Label(I18n.get("print.signature"));
        return new HBox(spacer, signature);
    }

    private static GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(8);
        return grid;
    }

    private static void row(GridPane grid, String labelKey, String value) {
        int index = grid.getRowCount();
        Label label = new Label(I18n.get(labelKey));
        label.getStyleClass().add("field-label");
        grid.add(label, 0, index);
        grid.add(new Label(value), 1, index);
    }

    private static String nonNull(String value) {
        return value == null ? "" : value;
    }
}
